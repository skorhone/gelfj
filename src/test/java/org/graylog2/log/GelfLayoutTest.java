package org.graylog2.log;

import static org.junit.Assert.*;

import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Test;

public class GelfLayoutTest {
	private static final String CLASS_NAME = GelfLayoutTest.class.getCanonicalName();

	@Test
	public void testFormat() {
		GelfLayout layout = new GelfLayout();
		layout.setExtractStacktrace(true);
		
		LoggingEvent event = new LoggingEvent(CLASS_NAME, Category.getInstance(GelfLayoutTest.class), 123L, Level.INFO,
				"Das Auto", new RuntimeException("Volkswagen"));
		String formatted = layout.format(event);

		assertTrue(formatted.contains("Das Auto"));
		assertTrue(formatted.contains("Volkswagen"));
	}
}