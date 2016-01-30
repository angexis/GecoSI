/**
 * Copyright (c) 2013 Simon Denier
 */
package net.gecosi.adapter.rxtx;


import java.io.IOException;

import jssc.SerialPort;
import jssc.SerialPortException;
import net.gecosi.internal.CommWriter;
import net.gecosi.internal.SiMessageQueue;
import net.gecosi.internal.SiPort;

/**
 * @author Simon Denier
 * @since Mar 10, 2013
 *
 */
public class RxtxPort implements SiPort {

	private SerialPort port;

	public RxtxPort(SerialPort port) {
		this.port = port;
	}
	
	public SerialPort getPort() {
		return port;
	}
	
	@Override
	public SiMessageQueue createMessageQueue() throws IOException, SerialPortException {
		SiMessageQueue messageQueue = new SiMessageQueue(10);
		port.addEventListener(new RxtxCommReader(port, messageQueue));
		return messageQueue;
	}
	
        @Override
	public CommWriter createWriter() throws IOException {
		return new RxtxCommWriter(port);
	}

        @Override
	public void setupHighSpeed() throws SerialPortException {
		setSpeed(38400);		
	}

        @Override
	public void setupLowSpeed() throws SerialPortException {
		setSpeed(4800);		
	}

	private void setSpeed(int baudRate) throws SerialPortException  {
		port.setParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,  SerialPort.PARITY_NONE);
	}
        
        @Override
	public void close() throws SerialPortException {
		// TODO: close streams?
		port.closePort();
	}
	
}
