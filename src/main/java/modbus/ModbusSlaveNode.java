package modbus;

import com.fazecast.jSerialComm.SerialPort;
import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.ModbusSlaveSet;
import com.serotonin.modbus4j.code.DataType;
import com.serotonin.modbus4j.exception.ErrorResponseException;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.ip.IpParameters;
import com.serotonin.modbus4j.locator.BaseLocator;
import com.serotonin.modbus4j.sero.messaging.MessagingExceptionHandler;
import javafx.application.Platform;
import settings.Settings;

public class ModbusSlaveNode {
    private PortWrapper portWrapper;
    private ModbusMaster master;


    public ModbusSlaveNode(SerialPort port) throws ModbusInitException {
        ModbusFactory modbusFactory = new ModbusFactory();
        portWrapper = new PortWrapper(port);
        if(port != null){
            master = modbusFactory.createRtuMaster(portWrapper);
            ModbusSlaveSet slave = modbusFactory.createTcpSlave(false);
        }else{
            IpParameters params = new IpParameters();
            params.setHost("127.0.0.1");
            params.setPort(502);
            master=modbusFactory.createTcpMaster(params,false);
        }
        master.setExceptionHandler(new MessagingExceptionHandler() {
            @Override
            public void receivedException(Exception e) {
                System.out.println(e.getCause().getMessage());
            }
        });
        master.setTimeout(200);
        master.init();
        Runnable target;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    Settings.nodes.forEach(node -> {
                        BaseLocator<Number> grossBaseLocator = BaseLocator.holdingRegister(node, 0, DataType.FOUR_BYTE_INT_UNSIGNED);
                        BaseLocator<Number> netBaseLocator = BaseLocator.holdingRegister(node, 2, DataType.FOUR_BYTE_INT_UNSIGNED);
                        try {

                            Number gross = master.getValue(grossBaseLocator);
                            //Number net = master.getValue(netBaseLocator);

                            //System.out.printf("Node %d gross:%d net:%d",node,gross.intValue(),net.intValue());
                            System.out.printf("Node %d gross:%d \n\r",node,gross.intValue());
                        } catch (ModbusTransportException e) {
                            e.printStackTrace();
                            //master.destroy();
                            //master.init();
                        } catch (ErrorResponseException e) {
                            e.printStackTrace();
                        }
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            System.out.println(e.getMessage());
                        }
                    });
                }
            }
        });
        thread.start();
        /*Platform.runLater(new Runnable() {
            @Override
            public void run() {
                while (true){
                    Settings.nodes.forEach(node -> {
                        BaseLocator<Number> grossBaseLocator = BaseLocator.holdingRegister(node, 0, DataType.FOUR_BYTE_INT_UNSIGNED);
                        BaseLocator<Number> netBaseLocator = BaseLocator.holdingRegister(node, 2, DataType.FOUR_BYTE_INT_UNSIGNED);
                        try {

                            Number gross = master.getValue(grossBaseLocator);
                            Number net = master.getValue(netBaseLocator);

                            System.out.printf("Node %d gross:%d net:%d",node,gross.intValue(),net.intValue());
                        } catch (ModbusTransportException e) {
                            e.printStackTrace();
                            //master.destroy();
                            //master.init();
                        } catch (ErrorResponseException e) {
                            e.printStackTrace();
                        }
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            System.out.println(e.getMessage());
                        }
                    });
                }
            }
        });*/


    }


}
