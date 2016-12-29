package org.graylog2.log;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.MDC;
import org.apache.log4j.NDC;
import org.apache.log4j.spi.LoggingEvent;
import org.graylog2.sender.GelfSender;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Anton Yakimov
 * @author Jochen Schalanda
 */
public class GelfAppenderTest {
	private static final String CLASS_NAME = GelfAppenderTest.class.getCanonicalName();
	private TestGelfSender gelfSender;
	private GelfAppender gelfAppender;

	@Before
	public void setUp() throws IOException {
		gelfSender = new TestGelfSender();
		gelfAppender = new GelfAppender() {
			@Override
			public GelfSender getGelfSender() {
				return gelfSender;
			}

			@Override
			public void append(LoggingEvent event) {
				super.append(event);
			}
		};
	}

	@After
	public void tearDown() {
		NDC.clear();
	}

	@Test
	public void ensureHostnameForMessage() {
		LoggingEvent event = new LoggingEvent(CLASS_NAME, Category.getInstance(GelfAppenderTest.class), 123L,
				Level.INFO, "Das Auto", new RuntimeException("Volkswagen"));
		gelfAppender.setOriginHost("example.com");
		gelfAppender.append(event);
		assertTrue(gelfSender.getLastMessage().contains("example.com"));
	}

	@Test
	public void handleNullInAppend() {
		LoggingEvent event = new LoggingEvent(CLASS_NAME, Category.getInstance(this.getClass()), 123L, Level.INFO, null,
				new RuntimeException("LOL"));
		gelfAppender.append(event);

		assertTrue(gelfSender.getLastMessage().contains("short_message"));
		assertTrue(gelfSender.getLastMessage().contains("full_message"));
	}

	@Test
	public void handleMDC() {
		gelfAppender.setAddExtendedInformation(true);
		LoggingEvent event = new LoggingEvent(CLASS_NAME, Category.getInstance(this.getClass()), 123L, Level.INFO, "",
				new RuntimeException("LOL"));
		MDC.put("foo", "bar");
		gelfAppender.append(event);

		assertTrue(gelfSender.getLastMessage().contains("foo"));
	}

	@Test
	public void handleNDC() {
		gelfAppender.setAddExtendedInformation(true);
		LoggingEvent event = new LoggingEvent(CLASS_NAME, Category.getInstance(this.getClass()), 123L, Level.INFO, "",
				new RuntimeException("LOL"));
		NDC.push("Foobar");
		gelfAppender.append(event);
		assertTrue(gelfSender.getLastMessage().contains("Foobar"));
	}

	@Test
	public void disableExtendedInformation() {
		gelfAppender.setAddExtendedInformation(false);
		LoggingEvent event = new LoggingEvent(CLASS_NAME, Category.getInstance(this.getClass()), 123L, Level.INFO, "",
				new RuntimeException("LOL"));
		MDC.put("foo", "bar");
		NDC.push("Foobar");
		gelfAppender.append(event);
		assertTrue(!gelfSender.getLastMessage().contains("bar"));
		assertTrue(!gelfSender.getLastMessage().contains("Foobar"));
	}

	@Test
	public void checkExtendedInformation() throws UnknownHostException, SocketException {
		gelfAppender.setAddExtendedInformation(true);
		LoggingEvent event = new LoggingEvent(CLASS_NAME, Category.getInstance(GelfAppenderTest.class), 123L,
				Level.INFO, "Das Auto", new RuntimeException("LOL"));
		gelfAppender.append(event);
		assertTrue(gelfSender.getLastMessage().contains(CLASS_NAME));
	}

	private class TestGelfSender implements GelfSender {
		private String lastMessage;

		public void sendMessage(String message) {
			this.lastMessage = message;
		}

		public void close() {
		}

		public String getLastMessage() {
			return lastMessage;
		}
	}
}
