// TODO: защита с помощью ключа
// TODO: сохранение ошибок в логи
import com.fazecast.jSerialComm.SerialPort;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.ip.IpParameters;
import com.serotonin.modbus4j.sero.util.ProgressiveTask;
import console.ConsoleView;
import database.Database;
import database.Excel;
import database.Indication;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import modbus.ModbusMasterNode;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import settings.Settings;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;
import java.util.List;

public class HopperScalesApplication extends Application {
    //static Map<String, String> properties = new HashMap<String, String>();
    //static List<Integer> hosts = new ArrayList<>();
    private Stage stage;
    private Settings settings;
    private ModbusMasterNode modbusSlaveNode;
    private SerialPort serialPort;
    ProgressiveTask scanTask = null;

    @Override
    public void start(Stage primaryStage) /*throws Exception*/ {
        EventBus.getDefault().register(this);
        //Thread.setDefaultUncaughtExceptionHandler(HopperScalesApplication::showError);
        Thread.currentThread().setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable t) {
                System.err.printf("Thread name: %s error: %s",thread.getName(), t.getMessage());
            }
        });
        this.stage = primaryStage;
        //final String[] args = getParameters().getRaw().toArray(new String[0]);
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
            System.err.println(e.getMessage());
        }finally {
            if (serialPort != null) {
                System.out.println("Serial port close");
                serialPort.closePort();
                serialPort = null;
            }
            SerialPort[] ports = SerialPort.getCommPorts();
            for (SerialPort port : ports) {
                System.out.println("[Найден Порт]"+port.getSystemPortName());
                if (port.getSystemPortName().equals(Settings.properties.get("port"))) {
                    serialPort = port;
                    serialPort.setBaudRate(Integer.parseInt(Settings.properties.get("speed")));
                    System.out.println("Serial порт " + Settings.properties.get("port")+" выбран");
                    break;
                }
            }
            if(serialPort==null){
                System.out.println("Serial порт " + Settings.properties.get("port")+" не найден. Укажите правильный порт в config.txt ");
                try {
                    modbusSlaveNode = new ModbusMasterNode(serialPort);
                } catch (ModbusInitException e) {
                    e.printStackTrace();
                }finally {

                }
            }else {
                //if (serialPort.openPort()) {
                    try {
                        modbusSlaveNode = new ModbusMasterNode(serialPort);
                    } catch (ModbusInitException e) {
                        e.printStackTrace();
                    }finally {

                    }
                //}
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

    public ModbusMasterNode.MyNodeScanListener nodeScanListener = new ModbusMasterNode.MyNodeScanListener() {
        List<Integer> result = new ArrayList();

        @Override
        public void nodeFound(int node) {
            result.add(node);
            System.out.printf("Found Node addresses: %d",node);
            System.out.println();
        }

        @Override
        public void completeFound(List<Integer> nodes) {
            System.out.printf("Найдено %d nodes",nodes.size());
            System.out.println();
            if(nodes.size() > 0){
                Settings.nodes.clear();
                StringBuilder sb = new StringBuilder();
                nodes.forEach(node -> {
                    sb.append(node).append(",");
                    Settings.nodes.add(node);
                });
               System.out.println("Добавте найденные nodes в config.txt в секцию nodes:"+sb.toString());
            }
            modbusSlaveNode.setTimeout(500);
            modbusSlaveNode.setPause(false);
        }

        @Override
        public void progressUpdate(float v) {
            System.out.print(".");
            //System.out.printf("progress %f%s",v,"%");
            //System.out.println();
        }

        @Override
        public void taskCancelled() {
            System.out.println("Scan canceled");
            this.completeFound(result);
        }

        @Override
        public void taskCompleted() {
            System.out.println("Scan completed");
            this.completeFound(result);
        }

        @Override
        public void clear(){
            result.clear();
        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventBus(String event) {
        try {
            JsonObject jsonObject = new Gson().fromJson(event, JsonObject.class);
            switch (jsonObject.get("cmd").getAsString()){
                case "scan_nodes":
                    if(scanTask == null || scanTask.isCompleted() )
                        scanTask =  modbusSlaveNode.scanForSlaveNodes(nodeScanListener);
                    else{
                        System.out.println("Уже сканируем");
                    }
                    break;
                case "scan_stop":
                    if(scanTask != null && !scanTask.isCompleted() )
                        scanTask.cancel();
                    else{
                        System.out.println("Сканер не запущен");
                    }
                    break;
                default:
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public static void main(String[] args) {
        try (Database database = new Database()) {
            database.initialize();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        launch(args);
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        EventBus.getDefault().unregister(this);
        modbusSlaveNode.stop();
    }
}
