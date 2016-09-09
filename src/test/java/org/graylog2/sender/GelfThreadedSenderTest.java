package org.graylog2.sender;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.graylog2.message.GelfMessage;
import org.graylog2.message.TestGelfMessage;
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
		configuration.setSendTimeout(100);
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
		sender.sendMessage(new TestGelfMessage());
	}

	@Test
	public void testQueueOfferFull() throws GelfSenderException {
		sender.sendMessage(new TestGelfMessage());
		sender.sendMessage(new TestGelfMessage());
		sender.sendMessage(new TestGelfMessage());
		try {
			sender.sendMessage(new TestGelfMessage());
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
			sender.sendMessage(new TestGelfMessage());
			fail("Exception is expected");
		} catch (GelfSenderException exception) {
			assertEquals(GelfSenderException.ERROR_CODE_SHUTTING_DOWN, exception.getErrorCode());
		}
	}

	@Test
	public void testQueueFinishesProcessingAfterClose() throws GelfSenderException {
		GelfMessage messageOne = new TestGelfMessage();
		GelfMessage messageTwo = new TestGelfMessage();
		sender.sendMessage(messageOne);
		sender.sendMessage(messageTwo);
		Executors.newSingleThreadScheduledExecutor().schedule(new Runnable() {
			public void run() {
				mockSender.allowSend();
				mockSender.allowSend();
			}
		}, 500, TimeUnit.MILLISECONDS);
		sender.close();

		assertSame(messageTwo, mockSender.getLastMessage());
	}

	public static class MockGelfSender implements GelfSender {
		private Semaphore semaphore;
		private GelfMessage lastMessage;

		public MockGelfSender() {
			semaphore = new Semaphore(0);
		}

		public void sendMessage(GelfMessage message) {
			try {
				semaphore.acquire();
			} catch (InterruptedException exception) {
			}
			this.lastMessage = message;
		}

		public GelfMessage getLastMessage() {
			return lastMessage;
		}

		public void allowSend() {
			semaphore.release();
		}

		public void close() {
		}
	}
}
