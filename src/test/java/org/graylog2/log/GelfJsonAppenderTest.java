package org.graylog2.log;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.graylog2.message.GelfMessage;
import org.graylog2.sender.GelfSender;
import org.junit.Before;
import org.junit.Test;

public class GelfJsonAppenderTest {

	private static final String CLASS_NAME = GelfJsonAppenderTest.class.getCanonicalName();
	private TestGelfSender gelfSender;
	private GelfAppender gelfAppender;

	@Before
	public void setUp() throws IOException {
		gelfSender = new TestGelfSender();

		gelfAppender = new GelfJsonAppender() {
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

	@Test
	public void testAppend() throws Exception {
		String message = "{\"simpleProperty\":\"hello gelf\", \"message\":\"test\"}";
		LoggingEvent event = new LoggingEvent(CLASS_NAME, Category.getInstance(this.getClass()), 123L, Level.INFO,
				message, new RuntimeException("LOL"));
		gelfAppender.append(event);

		assertThat("simpleProperty property exists in additional fields",
				(String) gelfSender.getLastMessage().getAdditionalFields().get("simpleProperty"), is("hello gelf"));
		assertThat("message property exists in additional fields",
				(String) gelfSender.getLastMessage().getAdditionalFields().get("message"), is("test"));
		assertThat("Full message is still JSON", (String) gelfSender.getLastMessage().getFullMessage(), is(message));
	}

	@Test
	public void testBrokenJasom() throws Exception {
		String message = "{\"simpleProperty\":\"hello gelf, \"message\":\"test}";
		LoggingEvent event = new LoggingEvent(CLASS_NAME, Category.getInstance(this.getClass()), 123L, Level.INFO,
				message, new RuntimeException("LOL"));
		gelfAppender.append(event);

		assertThat("No additional fields are created", gelfSender.getLastMessage().getAdditionalFields().size(), is(0));
		assertThat("Full message is the same", (String) gelfSender.getLastMessage().getFullMessage(), is(message));
	}

	private class TestGelfSender implements GelfSender {
		private GelfMessage lastMessage;

		public TestGelfSender() throws IOException {
		}

		public void sendMessage(GelfMessage message) {
			this.lastMessage = message;
		}

		public void close() {
		}

		public GelfMessage getLastMessage() {
			return lastMessage;
		}
	}

}