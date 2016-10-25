package org.graylog2.message;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class GelfMessageBuilderTest {
	@Test
	public void testInvalidHost() throws Exception {
		GelfMessageBuilderConfiguration configuration = new GelfMessageBuilderConfiguration();
		configuration.setOriginHost("");

		GelfMessageBuilder builder = new GelfMessageBuilder(configuration);
		builder.setFullMessage("message");

		try {
			builder.build();
			fail("Expected exception");
		} catch (Exception exception) {
			assertTrue(exception.getMessage().toLowerCase().contains("host"));
		}
	}

	@Test
	public void testNullMessageContent() throws Exception {
		GelfMessageBuilderConfiguration configuration = new GelfMessageBuilderConfiguration();
		configuration.setOriginHost("localhost");

		GelfMessageBuilder builder = new GelfMessageBuilder(configuration);
		builder.setFullMessage(null);

		try {
			builder.build();
			fail("Expected exception");
		} catch (Exception exception) {
			assertTrue(exception.getMessage().toLowerCase().contains("does not contain message content"));
		}
	}

	@Test
	public void testNullVersion() throws Exception {
		GelfMessageBuilderConfiguration configuration = new GelfMessageBuilderConfiguration();
		configuration.setOriginHost("localhost");

		GelfMessageBuilder builder = new GelfMessageBuilder(configuration);
		builder.setFullMessage(null);

		try {
			builder.build();
			fail("Expected exception");
		} catch (Exception exception) {
			assertTrue(exception.getMessage().toLowerCase().contains("does not contain message content"));
		}
	}
}
