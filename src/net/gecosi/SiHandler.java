/**
 * Copyright (c) 2013 Simon Denier
 */
package net.gecosi;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;

import java.io.IOException;
import java.util.TooManyListenersException;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * @author Simon Denier
 * @since Mar 12, 2013
 *
 */
public class SiHandler implements Runnable {

	private ArrayBlockingQueue<Si5DataFrame> dataQueue;
	private Thread thread;
	private SiDriver driver;
	private SiListener siListener;
	private long zerohour;
	
	public static void main(String[] args) {
		try {
			new SiHandler(new SiListener() {
				public void handleEcard(SiDataFrame dataFrame) {
					dataFrame.printString();
				}
				public void notify(CommStatus status) {
					System.out.println("Status -> " + status);					
				}
				public void notify(CommStatus errorStatus, String errorMessage) {
					System.out.println("Error -> " + errorStatus + " " + errorMessage);					
				}
			}).connect("/dev/tty.SLAB_USBtoUART");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void connect(String portname) throws IOException, TooManyListenersException {
		try {
			CommPortIdentifier portId = CommPortIdentifier.getPortIdentifier(portname);
			if( portId.isCurrentlyOwned() ) {
				siListener.notify(CommStatus.FATAL_ERROR, "Port owned by other app");
			} else {
				start();
				SerialPort port = (SerialPort) portId.open("GecoSI", 2000);
				driver = new SiDriver(new RxtxPort(port), this).start();
			}
		} catch (NoSuchPortException e) {
			siListener.notify(CommStatus.FATAL_ERROR, "Port unknowned");
		} catch (PortInUseException e) {
			siListener.notify(CommStatus.FATAL_ERROR, "Port in use");
		}
	}

	public SiHandler(SiListener siListener) {
		this.dataQueue = new ArrayBlockingQueue<Si5DataFrame>(5);
		this.siListener = siListener;
	}
	
	public void setZeroHour(long zerohour) {
		this.zerohour = zerohour;		
	}
	
	public void start() {
		thread = new Thread(this);
		thread.start();
	}
	
	public Thread stop() {
		driver.interrupt();
		thread.interrupt();
		return thread;
	}
	
	public boolean isAlive() {
		return thread.isAlive();
	}
	
	public void notify(Si5DataFrame data) {
		data.startingAt(zerohour);
		dataQueue.offer(data); // TODO check true
	}

	public void notify(CommStatus status) {
		siListener.notify(status);
	}

	public void notifyError(CommStatus errorStatus, String errorMessage) {
		siListener.notify(errorStatus, errorMessage);
	}
	
	public void run() {
		try {
			Si5DataFrame dataFrame;
			while( (dataFrame = dataQueue.take()) != null ) {
				siListener.handleEcard(dataFrame);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
