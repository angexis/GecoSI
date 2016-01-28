/**
 * Copyright (c) 2013 Simon Denier
 */
package net.gecosi.adapter.rxtx;

import jssc.SerialPort;
import jssc.SerialPortException;
import net.gecosi.internal.CommWriter;
import net.gecosi.internal.GecoSILogger;
import net.gecosi.internal.SiMessage;

/**
 * @author Simon Denier
 * @since Mar 10, 2013
 *
 */
public class RxtxCommWriter implements CommWriter {

	private SerialPort output;

	public RxtxCommWriter(SerialPort port) {
		this.output = port;
	}

	@Override
	public void write(SiMessage message) throws SerialPortException {
		GecoSILogger.log("SEND", message.toString());
		this.output.writeBytes(message.sequence());
	}

}
