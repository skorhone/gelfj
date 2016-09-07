package org.graylog2.logging;

import static org.junit.Assert.*;

import java.util.logging.Level;

import org.junit.Test;

public class GelfLogRecordTest {
	@Test
	public void testSetAndGetMultipleFields() {
		GelfLogRecord record = new GelfLogRecord(Level.FINE, "TEST");
		record.setField("one", "one");
		record.setField("two", "two");

		assertEquals("one", record.getField("one"));
		assertEquals("two", record.getField("two"));
		assertEquals("one", record.getFields().get("one"));
		assertEquals("two", record.getFields().get("two"));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testModifyFieldMap() {
		GelfLogRecord record = new GelfLogRecord(Level.FINE, "TEST");
		record.getFields().put("one", "one");
	}
}
