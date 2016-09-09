package org.graylog2.sender;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;

import org.graylog2.sender.GelfUDPSender.UDPBufferManager;
import org.junit.Test;

public class GelfUDPSenderTest {
	@Test
	public void testLongMessage() throws Exception {
		String longString = "01234567890123456789 ";
		for (int i = 0; i < 15; i++) {
			longString += longString;
		}
		UDPBufferManager bufferManager = new UDPBufferManager();
		ByteBuffer[] bytes = bufferManager.getUDPBuffers(longString);
		assertEquals(2, bytes.length);
	}

	@Test
	public void testShortMessage() throws Exception {
		UDPBufferManager bufferBuilder = new UDPBufferManager();
		ByteBuffer[] bytes = bufferBuilder.getUDPBuffers("very short");
		assertEquals(1, bytes.length);
	}
}