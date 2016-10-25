package org.graylog2.logging;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.logging.LogManager;

import org.junit.After;
import org.junit.Test;

public class JULPropertiesTest {
	@After
	public void cleanup() {
		LogManager.getLogManager().reset();
	}
	
	@Test
	public void testGetPropertyFromConfigurationFile() throws IOException {
		InputStream is = JULPropertiesTest.class.getResourceAsStream("logging-test.properties");
		LogManager logManager = LogManager.getLogManager();
		logManager.reset();
		logManager.readConfiguration(is);
		
		Properties systemProperties = new Properties();
		
		JULProperties properties = new JULProperties(logManager, systemProperties, "org.graylog2.logging.GelfHandler");
		
		assertEquals("test", properties.getProperty("facility"));
	}
	
	@Test
	public void testGetPropertyFromSystemProperties() throws IOException {
		InputStream is = JULPropertiesTest.class.getResourceAsStream("logging-test.properties");
		LogManager logManager = LogManager.getLogManager();
		logManager.reset();
		logManager.readConfiguration(is);
		
		Properties systemProperties = new Properties();
		systemProperties.put("org.graylog2.logging.GelfHandler.facility", "test2");
		
		JULProperties properties = new JULProperties(logManager, systemProperties, "org.graylog2.logging.GelfHandler");
		
		assertEquals("test2", properties.getProperty("facility"));
	}
	
	
	@Test
	public void testGetAllProperties() throws IOException {
		InputStream is = JULPropertiesTest.class.getResourceAsStream("logging-test.properties");
		LogManager logManager = LogManager.getLogManager();
		logManager.reset();
		logManager.readConfiguration(is);
		
		Properties systemProperties = new Properties();
		systemProperties.put("org.graylog2.logging.GelfHandler.additionalField.first", "one");
		systemProperties.put("org.graylog2.logging.GelfHandler.additionalField.second", "two");
		
		JULProperties properties = new JULProperties(logManager, systemProperties, "org.graylog2.logging.GelfHandler");

		Map<String, String> values = properties.getProperties("additionalField");
		assertEquals("one", values.get("first"));
		assertEquals("two", values.get("second"));
		assertEquals("bar", values.get("foo"));
		assertEquals("heck", values.get("bar"));
	}
}
