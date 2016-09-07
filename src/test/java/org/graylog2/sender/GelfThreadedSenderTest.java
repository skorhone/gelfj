package org.graylog2.sender;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

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
		sender = new GelfThreadedSender(mockSender, 100, 2);
	}

	@After
	public void teardown() {
		sender.close();
	}

	@Test
	public void testQueueOfferSuccess() {
		mockSender.allowSend();
		assertEquals(GelfSenderResult.OK, sender.sendMessage(new TestGelfMessage()));
	}

	@Test
	public void testQueueOfferFull() {
		assertEquals(GelfSenderResult.OK, sender.sendMessage(new TestGelfMessage()));
		assertEquals(GelfSenderResult.OK, sender.sendMessage(new TestGelfMessage()));
		assertEquals(GelfSenderResult.OK, sender.sendMessage(new TestGelfMessage()));
		assertEquals(GelfSenderResult.ERROR_CODE, sender.sendMessage(new TestGelfMessage()).getCode());
		mockSender.allowSend();
		mockSender.allowSend();
		mockSender.allowSend();
	}

	@Test
	public void testQueueOfferAfterClose() {
		sender.close();
		assertEquals(GelfSenderResult.MESSAGE_NOT_VALID_OR_SHUTTING_DOWN, sender.sendMessage(new TestGelfMessage()));
	}

	@Test
	public void testQueueFinishesProcessingAfterClose() {
		GelfMessage messageOne = new TestGelfMessage();
		GelfMessage messageTwo = new TestGelfMessage();
		assertEquals(GelfSenderResult.OK, sender.sendMessage(messageOne));
		assertEquals(GelfSenderResult.OK, sender.sendMessage(messageTwo));
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

		public GelfSenderResult sendMessage(GelfMessage message) {
			try {
				semaphore.acquire();
			} catch (InterruptedException exception) {
			}
			this.lastMessage = message;
			return GelfSenderResult.OK;
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
