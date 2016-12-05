package org.graylog2.util;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;

public class FieldsTest {
	@Test
	public void testNoFields() {
		Map<String, ? extends Object> fields = Fields.getFields(new NoFields());
		assertNull(fields);
	}
	
	@Test
	public void testHasFields() {
		Map<String, ? extends Object> fields = Fields.getFields(new HasFields());
		assertNotNull(fields);
		assertEquals("SIR", fields.get("YES"));
	}
	

	public static class NoFields {
	}
	
	public static class HasFields {
		public Map<String, Object> getFields() {
			return Collections.<String, Object>singletonMap("YES", "SIR");
		}
	}
}
