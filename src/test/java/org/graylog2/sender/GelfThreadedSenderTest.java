package org.graylog2.sender;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GelfThreadedSenderTest {
	private MockGelfSender mockSender;
	private GelfThreadedSender sender;

	@Before
	public void setup() {
		mockSender = new MockGelfSender();
		GelfSenderConfiguration configuration = new GelfSenderConfiguration();
		configuration.setThreadedQueueTimeout(100);
		configuration.setThreadedQueueMaxDepth(2);
		sender = new GelfThreadedSender(mockSender, configuration);
	}

	@After
	public void teardown() {
		sender.close();
	}

	@Test
	public void testQueueOfferSuccess() throws GelfSenderException {
		mockSender.allowSend();
		sender.sendMessage("");
	}

	@Test
	public void testQueueOfferFull() throws GelfSenderException {
		sender.sendMessage("");
		sender.sendMessage("");
		sender.sendMessage("");
		try {
			sender.sendMessage("");
			fail("Exception is expected");
		} catch (GelfSenderException exception) {
			assertEquals(GelfSenderException.ERROR_CODE_GENERIC_ERROR, exception.getErrorCode());
		} finally {
			mockSender.allowSend();
			mockSender.allowSend();
			mockSender.allowSend();
		}
	}

	@Test
	public void testQueueOfferAfterClose() {
		sender.close();
		try {
			sender.sendMessage("");
			fail("Exception is expected");
		} catch (GelfSenderException exception) {
			assertEquals(GelfSenderException.ERROR_CODE_SHUTTING_DOWN, exception.getErrorCode());
		}
	}

	@Test
	public void testQueueFinishesProcessingAfterClose() throws GelfSenderException {
		sender.sendMessage("1");
		sender.sendMessage("2");
		Executors.newSingleThreadScheduledExecutor().schedule(new Runnable() {
			public void run() {
				mockSender.allowSend();
				mockSender.allowSend();
			}
		}, 500, TimeUnit.MILLISECONDS);
		sender.close();

		assertEquals("2", mockSender.getLastMessage());
	}

	public static class MockGelfSender implements GelfSender {
		private Semaphore semaphore;
		private String lastMessage;

		public MockGelfSender() {
			semaphore = new Semaphore(0);
		}

		public void sendMessage(String message) {
			try {
				semaphore.acquire();
			} catch (InterruptedException exception) {
			}
			this.lastMessage = message;
		}

		public String getLastMessage() {
			return lastMessage;
		}

		public void allowSend() {
			semaphore.release();
		}

		public void close() {
		}
	}
}
