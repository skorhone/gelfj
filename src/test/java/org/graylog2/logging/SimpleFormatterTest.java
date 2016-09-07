package org.graylog2.logging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.Test;

public class SimpleFormatterTest {
	@Test
	public void testExtractStackTrace() {
		SimpleFormatter simpleFormatter = new SimpleFormatter(true);
		
		LogRecord record = new LogRecord(Level.WARNING, "MESSAGE");
		record.setThrown(new Exception());
		String message = simpleFormatter.format(record);
		
		assertTrue(message.contains("MESSAGE"));
		assertTrue(message.contains("Exception"));
		assertTrue(message.length() > 100);
	}

	@Test
	public void testDoNotExtractStackTrace() {
		SimpleFormatter simpleFormatter = new SimpleFormatter(false);
		
		LogRecord record = new LogRecord(Level.WARNING, "MESSAGE");
		record.setThrown(new Exception());
		String message = simpleFormatter.format(record);
		
		assertTrue(message.contains("MESSAGE"));
		assertFalse(message.contains("Exception"));
		assertEquals(7, message.length());
	}
	
	@Test
	public void testLogFormattingWithParameter() {
		SimpleFormatter simpleFormatter = new SimpleFormatter(false);
		
		LogRecord record = new LogRecord(Level.FINE, "logging param: {0}");
		record.setParameters(new Object[] {"param1"});

		assertEquals(simpleFormatter.format(record), "logging param: param1");
	}

	@Test
	public void testLogFormattingWithParameters() {
		SimpleFormatter simpleFormatter = new SimpleFormatter(false);
		
		LogRecord record = new LogRecord(Level.FINE, "logging params: {0} {1}");
		record.setParameters(new Object[] {new Integer(1), "param2"});

		assertEquals(simpleFormatter.format(record), "logging params: 1 param2");
	}

	@Test
	public void testLogFormattingWithPercentParameters() {
		SimpleFormatter simpleFormatter = new SimpleFormatter(false);
		
		LogRecord record = new LogRecord(Level.FINE, "logging params: %d %s");
		record.setParameters(new Object[] {new Integer(1), "param2"});

		assertEquals(simpleFormatter.format(record), "logging params: 1 param2");
	}

	@Test
	public void testLogFormattingWithPercentParameters_InvalidParameters() {
		SimpleFormatter simpleFormatter = new SimpleFormatter(false);
		
		LogRecord record = new LogRecord(Level.FINE, "logging params: %d %d");
		record.setParameters(new Object[] {new Integer(1), "param2"});

		assertEquals(simpleFormatter.format(record), "logging params: %d %d");
	}

	@Test
	public void testNullLogWithParameters() {
		SimpleFormatter simpleFormatter = new SimpleFormatter(false);
		
		LogRecord record = new LogRecord(Level.FINE, null);
		record.setParameters(new Object[] {new Integer(1), "param2"});

		assertEquals(simpleFormatter.format(record), "");
	}
}
