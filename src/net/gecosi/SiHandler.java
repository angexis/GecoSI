/**
 * Copyright (c) 2013 Simon Denier
 */
package net.gecosi;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;
import net.gecosi.adapter.logfile.LogFilePort;
import net.gecosi.adapter.rxtx.RxtxPort;
import net.gecosi.dataframe.SiDataFrame;
import net.gecosi.internal.GecoSILogger;
import net.gecosi.internal.SiDriver;

/**
 * @author Simon Denier
 * @since Mar 12, 2013
 *
 */
public class SiHandler implements Runnable {

	private ArrayBlockingQueue<SiDataFrame> dataQueue;

	private SiListener siListener;

	private long zerohour;

	private SiDriver driver;

	private Thread thread;


	public SiHandler(SiListener siListener) {
		this.dataQueue = new ArrayBlockingQueue<SiDataFrame>(5);
		this.siListener = siListener;
	}

	/**
	 * Returns the name of all the detected ports. Notice that the ports are
	 * just detected to have any device plugged, it is not necessarily a
	 * SportIdent device (and it may already be busy).
	 * 
	 * @return ["/dev/tty0"] or ["COM3", "COM8"]
	 */
	public static String[] listPortNames() {
		return SerialPortList.getPortNames();
	}

	public void setZeroHour(long zerohour) {
		this.zerohour = zerohour;
	}

	public void connect(String portname) {
		try {
			SerialPort port = new SerialPort(portname);
			GecoSILogger.open("######");
			GecoSILogger.logTime("Start " + portname);
			start();
			port.openPort();
			driver = new SiDriver(new RxtxPort(port), this).start();
		} catch (SerialPortException e) {
			siListener.notify(CommStatus.FATAL_ERROR, e.getExceptionType());
		}
		catch (IOException e) {
			siListener.notify(CommStatus.FATAL_ERROR, e.toString());
		}
	}
	public void readLog(String logFilename) throws IOException, SerialPortException {
		GecoSILogger.openOutStreamLogger();
		start();
		driver = new SiDriver(new LogFilePort(logFilename), this).start();
	}

	public void start() {
		thread = new Thread(this);
		thread.start();
	}

	public Thread stop() {
		if( driver != null ){
			driver.interrupt();
		}
		if( thread != null ){
			thread.interrupt();
		}
		return thread;
	}

	public boolean isAlive() {
		return thread != null && thread.isAlive();
	}

	public void notify(SiDataFrame data) {
		data.startingAt(zerohour);
		dataQueue.offer(data); // TODO check true
	}

	public void notify(CommStatus status) {
		GecoSILogger.log("!", status.name());
		siListener.notify(status);
	}

	public void notifyError(CommStatus errorStatus, String errorMessage) {
		GecoSILogger.error(errorMessage);
		siListener.notify(errorStatus, errorMessage);
	}

	public void run() {
		try {
			SiDataFrame dataFrame;
			while( (dataFrame = dataQueue.take()) != null ) {
				siListener.handleEcard(dataFrame);
			}
		} catch (InterruptedException e) {
			dataQueue.clear();
		}
	}

	public static void main(String[] args) {
		if( args.length == 0 ){
			printUsage();
			System.exit(0);
		}

		SiHandler handler = new SiHandler(new SiListener() {
			public void handleEcard(SiDataFrame dataFrame) {
				dataFrame.printString();
			}
			public void notify(CommStatus status) {
				System.out.println("Status -> " + status);
			}
			public void notify(CommStatus errorStatus, String errorMessage) {
				System.out.println("Error -> " + errorStatus + ": " + errorMessage);
				if (errorStatus == CommStatus.FATAL_ERROR) {
					System.exit(1);
				}
			}
		});

		if( args.length == 1 ){
			System.out.println("Found ports: " + Arrays.toString(listPortNames()));

			try {
				handler.connect(args[0]);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if( args.length == 2 && args[0].equals("--file") ){
			try {
				handler.readLog(args[1]);
			} catch (IOException | SerialPortException e) {
				e.printStackTrace();
			}
		} else {
			System.err.println("Unknown command line option");
			printUsage();
			System.exit(1);
		}
	}

	private static void printUsage() {
		System.out.println("Usage: java net.gecosi.SiHandler <serial portname> | --file <log filename>");
	}

}
