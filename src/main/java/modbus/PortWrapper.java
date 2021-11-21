package modbus;

import com.fazecast.jSerialComm.SerialPort;
import com.serotonin.modbus4j.serial.SerialPortWrapper;

import java.io.InputStream;
import java.io.OutputStream;


public class PortWrapper implements SerialPortWrapper {

    SerialPort serialPort;

    public PortWrapper(SerialPort serialPort){

        this.serialPort = serialPort;
    }


    /* (non-Javadoc)
     * @see com.serotonin.modbus4j.serial.SerialPortWrapper#close()
     */
    @Override
    public void close() throws Exception {
        serialPort.closePort();

    }

    /* (non-Javadoc)
     * @see com.serotonin.modbus4j.serial.SerialPortWrapper#open()
     */
    @Override
    public void open() throws Exception {
        serialPort.openPort();

    }

    /* (non-Javadoc)
     * @see com.serotonin.modbus4j.serial.SerialPortWrapper#getInputStream()
     */
    @Override
    public InputStream getInputStream() {
        return serialPort.getInputStream();
    }

    /* (non-Javadoc)
     * @see com.serotonin.modbus4j.serial.SerialPortWrapper#getOutputStream()
     */
    @Override
    public OutputStream getOutputStream() {
        return serialPort.getOutputStream();
    }

    /* (non-Javadoc)
     * @see com.serotonin.modbus4j.serial.SerialPortWrapper#getBaudRate()
     */
    @Override
    public int getBaudRate() {
        return serialPort.getBaudRate();
    }

    @Override
    public int getFlowControlIn() {
        return 0;
    }

    @Override
    public int getFlowControlOut() {
        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.modbus4j.serial.SerialPortWrapper#getStopBits()
     */
    @Override
    public int getStopBits() {
        return serialPort.getNumStopBits();
    }

    /* (non-Javadoc)
     * @see com.serotonin.modbus4j.serial.SerialPortWrapper#getParity()
     */
    @Override
    public int getParity() {
        return serialPort.getParity();
    }

    /* (non-Javadoc)
     * @see com.serotonin.modbus4j.serial.SerialPortWrapper#getDataBits()
     */
    @Override
    public int getDataBits() {
        return serialPort.getNumDataBits();
    }

}
