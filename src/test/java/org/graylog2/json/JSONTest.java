package org.graylog2.json;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

public class JSONTest {
	@Test
	public void testEncodeStringMap() {
		Map<String, String> map = new LinkedHashMap<String, String>();

		map.put("one", "one");
		map.put("two", "two");

		StringBuilder sb = new StringBuilder();
		JSON.encode((Object) map, sb);

		assertEquals("{\"one\": \"one\", \"two\": \"two\"}", sb.toString());
	}

	@Test
	public void testEncodeIntegerMap() {
		Map<Integer, Integer> map = new LinkedHashMap<Integer, Integer>();

		map.put(1, 1);
		map.put(2, 2);

		StringBuilder sb = new StringBuilder();
		JSON.encode((Object) map, sb);

		assertEquals("{\"1\": 1, \"2\": 2}", sb.toString());
	}

	@Test
	public void testEncodeAnyMap() {
		Map<Object, Object> map = new LinkedHashMap<Object, Object>();

		map.put(1, "one");
		map.put("two", 2);

		StringBuilder sb = new StringBuilder();
		JSON.encode((Object) map, sb);

		assertEquals("{\"1\": \"one\", \"two\": 2}", sb.toString());
	}

	@Test
	public void testEncodeNestedMap() {
		Map<String, Map<String, String>> map = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> nestedMap = new LinkedHashMap<String, String>();

		map.put("nested", nestedMap);
		nestedMap.put("one", "one");

		StringBuilder sb = new StringBuilder();
		JSON.encode((Object) map, sb);

		assertEquals("{\"nested\": {\"one\": \"one\"}}", sb.toString());
	}

	@Test
	public void testEncodeStringCollection() {
		Collection<String> collection = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();

		collection.add("one");
		collection.add("two");

		JSON.encode((Object) collection, sb);

		assertEquals("[\"one\", \"two\"]", sb.toString());
	}

	@Test
	public void testEncodeAnyCollection() {
		Collection<Object> collection = new ArrayList<Object>();
		StringBuilder sb = new StringBuilder();

		collection.add("one");
		collection.add(2);
		collection.add(new BigDecimal("100.00"));

		JSON.encode((Object) collection, sb);

		assertEquals("[\"one\", 2, 100.00]", sb.toString());
	}

	@Test
	public void testEncodeNestedCollection() {
		Collection<Collection<Object>> collection = new ArrayList<Collection<Object>>();
		Collection<Object> nestedCollection = new ArrayList<Object>();
		collection.add(nestedCollection);

		nestedCollection.add("one");
		nestedCollection.add(2);
		nestedCollection.add(new BigDecimal("100.00"));

		StringBuilder sb = new StringBuilder();
		JSON.encode((Object) collection, sb);

		assertEquals("[[\"one\", 2, 100.00]]", sb.toString());
	}

	@Test
	public void testEncodeStringArray() {
		String[] array = new String[2];
		StringBuilder sb = new StringBuilder();

		array[0] = "one";
		array[1] = "two";

		JSON.encode((Object) array, sb);

		assertEquals("[\"one\", \"two\"]", sb.toString());
	}

	@Test
	public void testEncodeNestedArray() {
		Object[][] array = new Object[1][3];

		array[0][0] = "one";
		array[0][1] = 2;
		array[0][2] = new BigDecimal("100.00");

		StringBuilder sb = new StringBuilder();
		JSON.encode((Object) array, sb);

		assertEquals("[[\"one\", 2, 100.00]]", sb.toString());
	}

	@Test
	public void testEncodeGELF() {
		Map<String, Object> gelfMessage = new LinkedHashMap<String, Object>();
		gelfMessage.put("version", "1.1");
		gelfMessage.put("timestamp", new BigDecimal("1000.00"));
		gelfMessage.put("short_message", "yes sir");
		gelfMessage.put("full_message", "no sir");
		gelfMessage.put("level", 2);
		gelfMessage.put("_myList", Arrays.asList(100));
		gelfMessage.put("_myMap", Collections.singletonMap("key", "value"));
		gelfMessage.put("_myArray", new Object[] { 1, "value" });

		StringBuilder sb = new StringBuilder();
		JSON.encode((Object) gelfMessage, sb);

		assertEquals(
				"{\"version\": \"1.1\", \"timestamp\": 1000.00, \"short_message\": \"yes sir\", \"full_message\": \"no sir\", \"level\": 2, \"_myList\": [100], \"_myMap\": {\"key\": \"value\"}, \"_myArray\": [1, \"value\"]}",
				sb.toString());
	}
}
