import com.fazecast.jSerialComm.SerialPort;
import com.serotonin.modbus4j.exception.ModbusInitException;
import console.ConsoleView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import modbus.ModbusSlaveNode;
import settings.Settings;

import java.net.URL;
import java.util.*;

public class HopperScalesApplication extends Application {
    static Map<String, String> properties = new HashMap<String, String>();
    static List<Integer> hosts = new ArrayList<>();
    private Stage stage;
    private Settings settings;
    private ModbusSlaveNode modbusSlaveNode;
    private SerialPort serialPort;

    @Override
    public void start(Stage primaryStage) /*throws Exception*/ {
        //Thread.setDefaultUncaughtExceptionHandler(HopperScalesApplication::showError);
        Thread.currentThread().setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable t) {
                System.err.printf("Thread name: %s error: %s",thread.getName(), t.getMessage());
            }
        });
        this.stage = primaryStage;
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

        try {
            settings = new Settings("config.txt");
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
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
                try {
                    modbusSlaveNode = new ModbusSlaveNode(serialPort);
                } catch (ModbusInitException e) {
                    e.printStackTrace();
                }finally {

                }
            }else {
                if (serialPort.openPort()) {
                    try {
                        modbusSlaveNode = new ModbusSlaveNode(serialPort);
                    } catch (ModbusInitException e) {
                        e.printStackTrace();
                    }finally {

                    }
                }
            }
        }


    }

    protected URL getStyleSheetUrl() {
        final String styleSheetName = "style.css";
        URL url = getClass().getResource(styleSheetName);
        if (url != null) {
            return url;
        }
        url = HopperScalesApplication.class.getResource(styleSheetName);
        if (url != null) {
            return url;
        }
        return null;
    }

    private static void showError(Thread t, Throwable e) {
        System.err.printf("Thread name: %s error: %s",t.getName(), e.getMessage());
        /*if (Platform.isFxApplicationThread()) {
            showErrorDialog(e);
        } else {
            System.err.println("An unexpected error occurred in "+t);

        }*/
    }

    public static void main(String[] args)throws Exception{
        launch(args);
    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }
}
