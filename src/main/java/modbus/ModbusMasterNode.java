package modbus;

import com.fazecast.jSerialComm.SerialPort;
import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.code.DataType;
import com.serotonin.modbus4j.exception.ErrorResponseException;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.ip.IpParameters;
import com.serotonin.modbus4j.locator.BaseLocator;
import com.serotonin.modbus4j.msg.ModbusRequest;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersRequest;
import com.serotonin.modbus4j.sero.messaging.MessagingExceptionHandler;
import com.serotonin.modbus4j.sero.util.ProgressiveTask;
import com.serotonin.modbus4j.sero.util.ProgressiveTaskListener;
import database.Database;
import settings.Settings;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ModbusMasterNode {
    private PortWrapper portWrapper;
    private ModbusMaster master;
    private boolean pause= false;

    public ModbusMasterNode(SerialPort port) throws ModbusInitException {
        ModbusFactory modbusFactory = new ModbusFactory();
        portWrapper = new PortWrapper(port);
        if(port != null){
            master = modbusFactory.createRtuMaster(portWrapper);
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
        master.setTimeout(500);
        master.init();
        Thread thread = new Thread(() -> {
            try {
                while (true) {
                    if (this.pause) {
                        Thread.sleep(100);
                        continue;
                    }
                    Settings.nodes.forEach(node -> {
                        BaseLocator<Number> grossBaseLocator = BaseLocator.holdingRegister(node, 0, DataType.FOUR_BYTE_INT_UNSIGNED);
                        BaseLocator<Number> netBaseLocator = BaseLocator.holdingRegister(node, 2, DataType.FOUR_BYTE_INT_UNSIGNED);
                        try {
                            Number gross = master.getValue(grossBaseLocator);
                            Number net = master.getValue(netBaseLocator);
                            try (Database database = new Database()) {
                                database.insertIndication(node, gross.intValue(), net.intValue());
                                System.out.printf("Node %d, gross = %d kg, net = %d kg\r\n", node, gross.intValue(), net.intValue());
                            } catch (SQLException exception) {
                                exception.printStackTrace();
                            }

                            //System.out.printf("Node %d gross:%d \n\r",node,gross.intValue());
                        } catch (ModbusTransportException e) {
                            System.out.format("Node %d ERROR %s \n\r",node,e.getCause().getMessage());
                            //System.err.println(e.getCause().getMessage());
                            //master.destroy();
                            //master.init();
                        } catch (ErrorResponseException e) {
                            System.err.println(e.getMessage());
                        }
                    });
                    System.out.println();
                    Thread.sleep(Settings.time);
                }
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public interface  MyNodeScanListener extends ProgressiveTaskListener {
        void nodeFound(int node);
        void completeFound(List<Integer> nodes);
        void clear();
    }

    public ProgressiveTask scanForSlaveNodes(final MyNodeScanListener listener) {

        listener.progressUpdate(0.0F);
        master.setTimeout(100);
        setPause(true);
        listener.clear();
        ProgressiveTask task = new ProgressiveTask(listener) {
            private int node = 1;

            protected void runImpl() {
                try {
                    master.send((ModbusRequest)(new ReadHoldingRegistersRequest(this.node, 0, 2)));
                    listener.nodeFound(this.node);
                } catch (ModbusTransportException e) {
                    //System.out.format("Node %d not found\n\r",this.node);
                    //System.err.println(e.getMessage());
                }

                this.declareProgress((float)this.node / 240.0F);
                ++this.node;
                if (this.node > 240) {
                    this.cancel();
                }

            }
        };
        (new Thread(task)).start();
        return task;
    }

    //public boolean isPause(){
    //    return this.pause;
    //}

    public void setPause(boolean pause){
        this.pause=pause;
    }

    public void stop(){
        master.destroy();
    }
}
