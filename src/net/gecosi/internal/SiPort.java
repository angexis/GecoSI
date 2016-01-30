/**
 * Copyright (c) 2013 Simon Denier
 */
package net.gecosi.internal;

import java.io.IOException;

import jssc.SerialPortException;

/**
 * @author Simon Denier
 * @since Mar 14, 2013
 *
 */
public interface SiPort {

	public SiMessageQueue createMessageQueue() throws IOException, SerialPortException;

	public CommWriter createWriter() throws IOException;

	public void setupHighSpeed() throws SerialPortException;
	
	public void setupLowSpeed() throws SerialPortException;

	public void close() throws SerialPortException;

}
