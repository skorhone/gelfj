package org.graylog2.logging;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.graylog2.message.GelfMessageBuilderConfiguration;
import org.junit.Before;
import org.junit.Test;

public class GelfFormatterTest {
	private GelfFormatter gelfFormatter;

	@Before
	public void setup() {
		GelfFormatterConfiguration gelfFormatterConfiguration = new GelfFormatterConfiguration();
		GelfMessageBuilderConfiguration gelfMessageBuilderConfiguration = new GelfMessageBuilderConfiguration();
		gelfFormatter = new GelfFormatter(gelfFormatterConfiguration, gelfMessageBuilderConfiguration);
	}

	@Test
	public void testLogFormattingWithParameter() {
		LogRecord record = new LogRecord(Level.FINE, "logging param: {0}");
		record.setParameters(new Object[] { "param1" });

		assertTrue(gelfFormatter.format(record).contains("logging param: param1"));
	}

	@Test
	public void testLogFormattingWithParameters() {
		LogRecord record = new LogRecord(Level.FINE, "logging params: {0} {1}");
		record.setParameters(new Object[] { new Integer(1), "param2" });

		assertTrue(gelfFormatter.format(record).contains("logging params: 1 param2"));
	}

	@Test
	public void testLogFormattingWithPercentParameters() {
		LogRecord record = new LogRecord(Level.FINE, "logging params: %d %s");
		record.setParameters(new Object[] { new Integer(1), "param2" });

		assertTrue(gelfFormatter.format(record).contains("logging params: 1 param2"));
	}

	@Test
	public void testLogFormattingWithPercentParameters_InvalidParameters() {
		LogRecord record = new LogRecord(Level.FINE, "logging params: %d %d");
		record.setParameters(new Object[] { new Integer(1), "param2" });

		assertTrue(gelfFormatter.format(record).contains("logging params: %d %d"));
	}

	@Test
	public void testNullLogWithParameters() {
		LogRecord record = new LogRecord(Level.FINE, null);
		record.setParameters(new Object[] { new Integer(1), "param2" });
	}

	@Test
	public void testResolveFromResourceBundle() {
		LogRecord record = new LogRecord(Level.FINE, "X100");
		record.setResourceBundle(new ResourceBundle() {
			@Override
			protected Object handleGetObject(String key) {
				if (key.equals("X100")) {
					return "Message from bundle";
				}
				return null;
			}

			@Override
			public Enumeration<String> getKeys() {
				return Collections.enumeration(Collections.singleton("X100"));
			}
		});
		assertTrue(gelfFormatter.format(record).contains("Message from bundle"));
	}
}
