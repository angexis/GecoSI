/**
 * Copyright (c) 2013 Simon Denier
 */
package net.gecosi.internal;

import jssc.SerialPortException;

/**
 * @author Simon Denier
 * @since Mar 15, 2013
 *
 */
public interface CommWriter {

	public void write(SiMessage message) throws SerialPortException;

}
