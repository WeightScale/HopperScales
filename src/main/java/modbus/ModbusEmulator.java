package modbus;

import com.serotonin.modbus4j.*;
import com.serotonin.modbus4j.code.DataType;
import com.serotonin.modbus4j.code.RegisterRange;
import com.serotonin.modbus4j.exception.IllegalDataAddressException;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.ip.IpParameters;
import com.serotonin.modbus4j.locator.BaseLocator;

public class ModbusEmulator {
    private boolean runnable=true;
    private static ModbusSlaveSet slave;
    private static BasicProcessImage processImage;
    static BaseLocator<Number> grossBaseLocator = BaseLocator.holdingRegister(1, 0, DataType.FOUR_BYTE_INT_UNSIGNED);
    static BaseLocator<Number> netBaseLocator = BaseLocator.holdingRegister(1, 2, DataType.FOUR_BYTE_INT_UNSIGNED);

    public static void main(String[] args) throws ModbusInitException {
        IpParameters params = new IpParameters();
        params.setHost("127.0.0.1");
        params.setPort(502);
        //ModbusFactory modbusFactory = new ModbusFactory();
        slave = new ModbusFactory().createTcpSlave(false);
        processImage = getProcessImages(1);
        slave.addProcessImage(processImage);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    slave.start();
                }catch (ModbusInitException e) {
                    System.err.println(e.getMessage());
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
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

    static void updateProcessImage(BasicProcessImage processImage) throws IllegalDataAddressException {
        //processImage.setHoldingRegister(0, (short) new Random(100).nextInt());
        //int randomNum = ThreadLocalRandom.current().nextInt(1, 100000 + 1);
        int randomNum = processImage.getNumeric(RegisterRange.HOLDING_REGISTER, 0, DataType.FOUR_BYTE_INT_UNSIGNED).intValue();
        processImage.setNumeric(RegisterRange.HOLDING_REGISTER, 0, DataType.FOUR_BYTE_INT_UNSIGNED, ++randomNum);
        //processImage.setHoldingRegister(0, (short) randomNum);
    }

    static BasicProcessImage getProcessImages(int slaveId) {
        BasicProcessImage processImage = new BasicProcessImage(slaveId);
        processImage.setNumeric(RegisterRange.HOLDING_REGISTER, 0, DataType.FOUR_BYTE_INT_UNSIGNED, 3);
        //processImage.addListener(new BasicProcessImageListener());
        return processImage;
    }

}
