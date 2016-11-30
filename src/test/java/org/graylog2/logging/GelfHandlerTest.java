package org.graylog2.logging;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

/**
 * @author lkmikkel
 */
public class GelfHandlerTest {
	private MockGelfHandlerSender gelfSender;

	@Before
	public void setUp() throws IOException {
		gelfSender = new MockGelfHandlerSender();
		InputStream is = GelfHandlerTest.class.getResourceAsStream("logging-test.properties");
		LogManager.getLogManager().readConfiguration(is);
		for (Handler handler : LogManager.getLogManager().getLogger("").getHandlers()) {
			if (handler instanceof GelfHandler) {
				GelfHandler gelfHandler = (GelfHandler) handler;
				gelfHandler.setGelfSender(gelfSender);
			}
		}
	}

	@Test
	public void handleNullMessage() {
		Logger myLogger = Logger.getLogger("testNullMessage");

		myLogger.log(Level.FINE, (String) null);

		assertThat("Message short message", gelfSender.getLastMessage().getShortMessage(), notNullValue());
		assertThat("Message full message", gelfSender.getLastMessage().getFullMessage(), nullValue());
	}

	@Test
	public void handleAdditionalField() {
		Logger myLogger = Logger.getLogger("testAdditionalField");
		myLogger.log(Level.FINE, "test additional field");

		assertEquals("bar", gelfSender.getLastMessage().getAdditionalFields().get("foo"));
		assertNull(gelfSender.getLastMessage().getAdditionalFields().get("non-existent"));
	}

	@Test
	public void handleStackTraces() {
		Logger myLogger = Logger.getLogger("testStackTraces");
		myLogger.log(Level.FINE, "test stacktrace:", new RuntimeException("test"));

		Pattern regex = Pattern.compile(
				"^.*java\\.lang\\.RuntimeException: test.*at org\\.graylog2\\.logging\\.GelfHandlerTest\\.handleStackTraces.*$",
				Pattern.MULTILINE | Pattern.DOTALL);
		assertTrue(regex.matcher(gelfSender.getLastMessage().getFullMessage()).matches());
	}
}
