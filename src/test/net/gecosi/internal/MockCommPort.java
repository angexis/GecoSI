/**
 * Copyright (c) 2013 Simon Denier
 */
package net.gecosi.internal;

import java.io.IOException;
import java.util.Arrays;

import jssc.SerialPortException;

/**
 * @author Simon Denier
 * @since Mar 14, 2013
 *
 */
public class MockCommPort implements SiPort {

	private MockComm comm;
	
	private SiMessageQueue messageQueue;
	
	/**
	 * Constructor for timeout (empty queue)
	 */
	public MockCommPort() {
		this(new SiMessage[0]);
	}

	public MockCommPort(SiMessage[] siMessages) {
		comm = new MockComm();
		messageQueue = new SiMessageQueue(siMessages.length + 1, 1);
		messageQueue.addAll(Arrays.asList(siMessages));
		
	}

	public SiMessageQueue createMessageQueue() throws IOException {
		return messageQueue;
	}

	public CommWriter createWriter() throws IOException {
		return this.comm;
	}

	public class MockComm implements CommWriter {
		public void write(SiMessage message) throws SerialPortException {}
	}

	public void setupHighSpeed() {}

	public void setupLowSpeed() {}

	public void close() {
		// TODO test always closed/called
	}
	
}
