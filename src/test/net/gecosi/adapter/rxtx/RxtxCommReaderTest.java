/**
 * Copyright (c) 2013 Simon Denier
 */
package net.gecosi.adapter.rxtx;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortException;
import net.gecosi.adapter.rxtx.RxtxCommReader;
import net.gecosi.internal.GecoSILogger;
import net.gecosi.internal.SiMessage;
import net.gecosi.internal.SiMessageQueue;

/**
 * @author Simon Denier
 * @since Jun 10, 2013
 *
 */
public class RxtxCommReaderTest {
        @Mock
        private SerialPort serialPort;
	
	@Mock
	private SiMessageQueue messageQueue;

	@Mock
	private SerialPortEvent triggerEvent;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		GecoSILogger.open();
	}
	
	public RxtxCommReader subject() {
		return new RxtxCommReader(serialPort, messageQueue);
	}
	
	@Test
	public void nomicalCase() throws SerialPortException {
		byte[] testInput = new byte[]{0x02, (byte) 0xF0, 0x03, 0x00, 0x01, 0x4D, 0x0D, 0x11, 0x03};
		testReaderOutput(new byte[][]{ testInput }, testInput, subject());
	}

	@Test
	public void messageInTwoFragments() throws SerialPortException {
		byte[] testInput1 = new byte[]{0x02, (byte) 0xF0, 0x03};
		byte[] testInput2 = new byte[]{0x00, 0x01, 0x4D, 0x0D, 0x11, 0x03};
		byte[] testOutput = new byte[]{0x02, (byte) 0xF0, 0x03, 0x00, 0x01, 0x4D, 0x0D, 0x11, 0x03};
		testReaderOutput(new byte[][]{ testInput1, testInput2 }, testOutput, subject());
	}

	@Test
	public void messageInMultipleFragments() throws SerialPortException {
		byte[] testInput1 = new byte[]{0x02, (byte) 0xF0, 0x03};
		byte[] testInput2 = new byte[]{0x00, 0x01, 0x4D, 0x0D};
		byte[] testInput3 = new byte[]{0x11, 0x03};
		byte[] testOutput = new byte[]{0x02, (byte) 0xF0, 0x03, 0x00, 0x01, 0x4D, 0x0D, 0x11, 0x03};
		testReaderOutput(new byte[][]{ testInput1, testInput2, testInput3 }, testOutput, subject());
	}
	
	@Test
	public void firstFragmentWithoutLengthPrefix() throws SerialPortException {
		byte[] testInput1 = new byte[]{0x02};
		byte[] testInput2 = new byte[]{(byte) 0xF0, 0x03, 0x00, 0x01, 0x4D, 0x0D};
		byte[] testInput3 = new byte[]{0x11, 0x03};
		byte[] testOutput = new byte[]{0x02, (byte) 0xF0, 0x03, 0x00, 0x01, 0x4D, 0x0D, 0x11, 0x03};
		testReaderOutput(new byte[][]{ testInput1, testInput2, testInput3 }, testOutput, subject());
	}

	@Test
	public void zeroDataMessage() throws SerialPortException {
		byte[] testInput1 = new byte[]{0x02};
		byte[] testInput2 = new byte[]{(byte) 0xF0, 0x00, (byte) 0xF0, 0x00, 0x03};
		byte[] testOutput = new byte[]{0x02, (byte) 0xF0, 0x00, (byte) 0xF0, 0x00, 0x03};
		testReaderOutput(new byte[][]{ testInput1, testInput2 }, testOutput, subject());
	}
	
	@Test
	public void emptyMessage() throws SerialPortException {
		RxtxCommReader subject = subject();
		when(serialPort.readBytes()).thenReturn(new byte[0]);
		subject.serialEvent(triggerEvent);
		verifyZeroInteractions(messageQueue);
	}
	
	@Test
	public void shortMessage() throws SerialPortException {
		byte[] testInput = new byte[]{0x15};
		testReaderOutput(new byte[][]{ testInput }, testInput, subject());
	}
	
	@Test
	public synchronized void timeoutResetsAccumulator() throws SerialPortException {
		try {
			RxtxCommReader subject = new RxtxCommReader(serialPort, messageQueue, 1);
			byte[] testInput1 = new byte[]{0x02, (byte) 0xF0, 0x03};
		        when(serialPort.readBytes()).thenReturn(testInput1);
			subject.serialEvent(triggerEvent);

			wait(2);
			byte[] testInput2 = new byte[]{0x00, 0x01, 0x4D, 0x0D, 0x11, 0x03};
                        when(serialPort.readBytes()).thenReturn(testInput2);
			subject.serialEvent(triggerEvent);
		
			verifyZeroInteractions(messageQueue);
			
			wait(2);
			byte[] testInput = new byte[]{0x02, (byte) 0xF0, 0x03, 0x00, 0x01, 0x4D, 0x0D, 0x11, 0x03};
			testReaderOutput(new byte[][]{ testInput }, testInput, subject);
		} catch (InterruptedException e) {
			fail();
		}
	}

	@Test
	public synchronized void tooLongFragmentResetsAccumulator() throws SerialPortException {
		RxtxCommReader subject = new RxtxCommReader(serialPort, messageQueue, 1);
		byte[] testInput1 = new byte[]{0x02, (byte) 0xF0, 0x03};
                when(serialPort.readBytes()).thenReturn(testInput1);
		subject.serialEvent(triggerEvent);

		byte[] testInput2 = new byte[]{0x00, 0x01, 0x4D, 0x0D, 0x11, 0x03, (byte) 0xFF};
                when(serialPort.readBytes()).thenReturn(testInput2);
		subject.serialEvent(triggerEvent);
		
		verifyZeroInteractions(messageQueue);
		
		try {
			wait(2);
			byte[] testInput = new byte[]{0x02, (byte) 0xF0, 0x03, 0x00, 0x01, 0x4D, 0x0D, 0x11, 0x03};
			testReaderOutput(new byte[][]{ testInput }, testInput, subject);
		} catch (InterruptedException e) {
			fail();
		}
	}
	
	private void testReaderOutput(byte[][] testInputs, byte[] expectedOutput, RxtxCommReader subject) throws SerialPortException {
		try {
			for (int i = 0; i < testInputs.length; i++) {
	                        when(serialPort.readBytes()).thenReturn(testInputs[i]);
				subject.serialEvent(triggerEvent);
			}
			ArgumentCaptor<SiMessage> message = ArgumentCaptor.forClass(SiMessage.class);
			verify(messageQueue).put(message.capture());
			assertThat(message.getValue().sequence(), equalTo(expectedOutput));
		} catch (InterruptedException e) {
			fail();
		}
	}

}
