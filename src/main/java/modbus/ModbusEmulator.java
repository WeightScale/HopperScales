package modbus;

import com.fazecast.jSerialComm.SerialPort;
import com.serotonin.modbus4j.*;
import com.serotonin.modbus4j.code.DataType;
import com.serotonin.modbus4j.code.RegisterRange;
import com.serotonin.modbus4j.exception.IllegalDataAddressException;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.locator.BaseLocator;
import console.ConsoleView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ModbusEmulator extends Application {
    private boolean runnable=true;
    public static Map<String, String> properties = new HashMap<String, String>();
    public static List<Integer> nodes = new ArrayList<>();
    private PortWrapper portWrapper;
    private ModbusSlaveSet slave;
    private SerialPort serialPort;
    //private static BasicProcessImage processImage;
    static BaseLocator<Number> grossBaseLocator = BaseLocator.holdingRegister(1, 0, DataType.FOUR_BYTE_INT_UNSIGNED);
    static BaseLocator<Number> netBaseLocator = BaseLocator.holdingRegister(1, 2, DataType.FOUR_BYTE_INT_UNSIGNED);

    @Override
    public void start(Stage primaryStage) throws Exception {
        Thread.currentThread().setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable t) {
                t.printStackTrace();
            }
        });

        final String[] args = getParameters().getRaw().toArray(new String[0]);
        final ConsoleView console = new ConsoleView();
        final Scene scene = new Scene(console,800,600);
        final URL styleSheetUrl = getStyleSheetUrl();
        if (styleSheetUrl != null) {
            scene.getStylesheets().add(styleSheetUrl.toString());
        }
        primaryStage.setTitle(getClass().getSimpleName());
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            //if (WindowEvent.WINDOW_CLOSE_REQUEST.equals(e.getEventType())) {
            Platform.exit();
            //}
        });
        primaryStage.show();

        System.setOut(console.getOut());
        System.setIn(console.getIn());
        System.setErr(console.getOut());

        try{
            File file = new File("config_emulator.txt");
            InputStream inputStream = null;
            StringBuilder sb = new StringBuilder();
            inputStream = new FileInputStream(file);
            for (int ch; (ch = inputStream.read()) != -1; ) {
                sb.append((char) ch);
            }

            String config = sb.toString();
            String[] pairs = config.split("\r\n");
            for (int i=0;i<pairs.length;i++) {
                String pair = pairs[i];
                String[] keyValue = pair.split(":");
                properties.put(keyValue[0], keyValue[1]);
            }
            if(properties.containsKey("hosts")){
                String str = properties.get("hosts");
                nodes = Stream.of(str.replace(" ", "").split(",")).map(Integer::valueOf).collect(Collectors.toList());
            }

            if (serialPort != null) {
                System.out.println("Serial port close");
                serialPort.closePort();
                serialPort = null;
            }
            SerialPort[] ports = SerialPort.getCommPorts();
            for (SerialPort port : ports) {
                System.out.println("[Найден Порт]"+port.getSystemPortName());
                if (port.getSystemPortName().equals(properties.get("port"))) {
                    serialPort = port;
                    serialPort.setBaudRate(Integer.parseInt(properties.get("speed")));
                    System.out.println("Serial порт " + properties.get("port")+" выбран");
                    break;
                }
            }
            if(serialPort==null){
                System.out.println("Serial порт " + properties.get("port")+" не найден. Укажите правильный порт в config.txt ");
                System.out.println("Запущен TCP Slave ");
                slave = new ModbusFactory().createTcpSlave(false);
            }else {
                portWrapper = new PortWrapper(serialPort);
                slave = new ModbusFactory().createRtuSlave(portWrapper);
            }

            nodes.forEach(node -> {
                slave.addProcessImage(getProcessImages(node));
            });

            Thread threadSlaveStart = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        slave.start();
                    }catch (ModbusInitException e) {
                        System.err.println(e.getMessage());
                    }
                }
            });
            threadSlaveStart.setDaemon(true);
            threadSlaveStart.start();

            Thread threadUpdateImage = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        for (ProcessImage processImage : slave.getProcessImages()) {
                            try {
                                updateProcessImage((BasicProcessImage) processImage);
                            } catch (IllegalDataAddressException e) {
                                e.printStackTrace();
                            }
                        }
                        System.out.println();
                        synchronized (slave) {
                            try {
                                slave.wait(Integer.parseInt(properties.getOrDefault("time","1000")));
                            } catch (InterruptedException e) {
                                System.err.println(e.getMessage());
                            }
                        }
                    }
                }
            });
            threadUpdateImage.setDaemon(true);
            threadUpdateImage.start();
        }catch (Exception e){
            System.out.println(e.getMessage());
        }

    }

    public static void main(String[] args) throws ModbusInitException {
        launch(args);
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        slave.stop();
    }

    static void updateProcessImage(BasicProcessImage processImage) throws IllegalDataAddressException {
        //processImage.setHoldingRegister(0, (short) new Random(100).nextInt());
        //int randomNum = ThreadLocalRandom.current().nextInt(1, 100000 + 1);
        int gross = processImage.getNumeric(RegisterRange.HOLDING_REGISTER, 0, DataType.FOUR_BYTE_INT_UNSIGNED).intValue();
        int net = processImage.getNumeric(RegisterRange.HOLDING_REGISTER, 2, DataType.FOUR_BYTE_INT_UNSIGNED).intValue();
        processImage.setNumeric(RegisterRange.HOLDING_REGISTER, 0, DataType.FOUR_BYTE_INT_UNSIGNED, ++gross);
        processImage.setNumeric(RegisterRange.HOLDING_REGISTER, 2, DataType.FOUR_BYTE_INT_UNSIGNED, ++net);
        //processImage.setHoldingRegister(0, (short) randomNum);
        System.out.printf("Node %d, gross = %d kg, net = %d kg\n\r", processImage.getSlaveId(), gross, net);

    }

    static BasicProcessImage getProcessImages(int slaveId) {
        BasicProcessImage processImage = new BasicProcessImage(slaveId);
        processImage.setNumeric(RegisterRange.HOLDING_REGISTER, 0, DataType.FOUR_BYTE_INT_UNSIGNED, 1);
        processImage.setNumeric(RegisterRange.HOLDING_REGISTER, 2, DataType.FOUR_BYTE_INT_UNSIGNED, 2);
        //processImage.addListener(new BasicProcessImageListener());
        return processImage;
    }

    protected URL getStyleSheetUrl() {
        final String styleSheetName = "../style.css";
        URL url = getClass().getResource(styleSheetName);
        if (url != null) {
            return url;
        }
        url = ModbusEmulator.class.getResource(styleSheetName);
        if (url != null) {
            return url;
        }
        return null;
    }
}
