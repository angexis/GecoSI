/**
 * Copyright (c) 2013 Simon Denier
 */
package net.gecosi.internal;

/**
 * @author Simon Denier
 * @since Mar 21, 2013
 *
 */
public class InvalidMessage extends Exception {

	private static final long serialVersionUID = 8582804744437652882L;
	private SiMessage receivedMessage;

	public InvalidMessage(SiMessage receivedMessage) {
		this.receivedMessage = receivedMessage;
	}
	
	public SiMessage receivedMessage() {
		return receivedMessage;
	}
	
}
