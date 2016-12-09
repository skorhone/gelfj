package org.graylog2.util;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Map;

import org.graylog2.field.ReflectionFieldExtractor;
import org.junit.Test;

public class ReflectionFieldExtractorTest {
	private ReflectionFieldExtractor extractor = new ReflectionFieldExtractor();
	
	@Test
	public void testNoFields() {
		Map<String, ? extends Object> fields = extractor.getFields(new NoFields());
		assertNull(fields);
	}
	
	@Test
	public void testHasFields() {
		Map<String, ? extends Object> fields = extractor.getFields(new HasFields());
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
