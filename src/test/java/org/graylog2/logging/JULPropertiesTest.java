package org.graylog2.logging;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

import org.junit.Test;

public class JULPropertiesTest {
	@Test
	public void testGetProperty() throws IOException {
		InputStream is = JULPropertiesTest.class.getResourceAsStream("logging-test.properties");
		LogManager logManager = LogManager.getLogManager();
		logManager.readConfiguration(is);
		
		JULProperties properties = new JULProperties(logManager, "org.graylog2.logging.GelfHandler");
		
		assertEquals("test", properties.getProperty("facility"));
	}	
}
