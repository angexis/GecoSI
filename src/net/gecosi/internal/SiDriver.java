/**
 * Copyright (c) 2013 Simon Denier
 */
package net.gecosi.internal;

import java.io.IOException;
import java.util.TooManyListenersException;
import java.util.concurrent.TimeoutException;

import jssc.SerialPortException;
import net.gecosi.CommStatus;
import net.gecosi.SiHandler;

/**
 * @author Simon Denier
 * @since Feb 13, 2013
 *
 */
public class SiDriver implements Runnable {

	private SiPort siPort;
	private CommWriter writer;
	private SiMessageQueue messageQueue;
	private Thread thread;
	private SiHandler siHandler;

	public SiDriver(SiPort siPort, SiHandler siHandler) throws TooManyListenersException, IOException, SerialPortException {
		this.siPort = siPort;
		this.messageQueue = siPort.createMessageQueue();
		this.writer = siPort.createWriter();
		this.siHandler = siHandler;
	}

	public SiDriver start() {
		thread = new Thread(this);
		thread.start();
		return this;
	}

	public void interrupt() {
		thread.interrupt();
	}

	public void run() {
		try {
			SiDriverState currentState = startupBootstrap();
			while( isAlive(currentState) ) {
				GecoSILogger.stateChanged(currentState.name());
				currentState = currentState.receive(messageQueue, writer, siHandler);
			}
			if( currentState.isError() ) {
				siHandler.notifyError(CommStatus.FATAL_ERROR, currentState.status());
			}
		} catch (InterruptedException e) {
			// normal way out
		} catch (Exception e) {
			e.printStackTrace();
			GecoSILogger.error(" #run# " + e.toString());
		} finally {
	            try {
	                stop();
	            } catch (SerialPortException e) {
	                GecoSILogger.error(" #run# failed to stop" + e.toString());
	            }
		}
	}

	private boolean isAlive(SiDriverState currentState) {
		return ! (thread.isInterrupted() || currentState.isError());
	}

	private SiDriverState startupBootstrap()
			throws IOException, InterruptedException, InvalidMessage, SerialPortException {
		try {
			siHandler.notify(CommStatus.STARTING);
			siPort.setupHighSpeed();
			return startup();
		} catch (TimeoutException e) {
			try {
				siPort.setupLowSpeed();
				return startup();
			} catch (TimeoutException e1) {
				return SiDriverState.STARTUP_TIMEOUT;
			}
		}
	}

	private SiDriverState startup()
			throws IOException, InterruptedException, TimeoutException, InvalidMessage, SerialPortException {
		SiDriverState currentState = SiDriverState.STARTUP.send(writer, siHandler).receive(messageQueue, writer, siHandler);
		return currentState;
	}

	private void stop() throws SerialPortException {
		siPort.close();
		siHandler.notify(CommStatus.OFF);
		GecoSILogger.close();
	}

}
