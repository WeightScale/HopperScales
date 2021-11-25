package modbus;

import com.serotonin.modbus4j.*;
import com.serotonin.modbus4j.code.DataType;
import com.serotonin.modbus4j.code.RegisterRange;
import com.serotonin.modbus4j.exception.IllegalDataAddressException;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.ip.IpParameters;
import com.serotonin.modbus4j.locator.BaseLocator;
import console.ConsoleView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import settings.Settings;

import java.net.URL;

// TODO: не завершается программа
public class ModbusEmulator extends Application {
    private boolean runnable=true;
    private ModbusSlaveSet slave;
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

        slave = new ModbusFactory().createTcpSlave(false);
        //processImage = getProcessImages(1);
        slave.addProcessImage(getProcessImages(1));
        slave.addProcessImage(getProcessImages(2));
        slave.addProcessImage(getProcessImages(3));
        slave.addProcessImage(getProcessImages(4));
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

                    synchronized (slave) {
                        try {
                            slave.wait(500);
                        } catch (InterruptedException e) {
                            System.err.println(e.getMessage());
                        }
                    }
                }
            }
        });
        threadUpdateImage.setDaemon(true);
        threadUpdateImage.start();
    }

    public static void main(String[] args) throws ModbusInitException {
        launch(args);
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
