package org.graylog2.message;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Date;
import java.util.Map;

import org.json.simple.JSONValue;
import org.junit.Test;

public class GelfMessageTest {
	@Test
	public void testAdditionalFieldsIds() {
		GelfMessage message = new GelfMessage();
		message.setShortMessage("Short");
		message.setFullMessage("Long");
		message.setJavaTimestamp(new Date().getTime());
		message.setLevel("1");
		message.addField("_id", "typos in my closet");

		String data = message.toJson();
		Map resultingMap = (Map) JSONValue.parse(data);
		assertNotNull(resultingMap.get("__id"));
	}

	@Test
	public void testAdditionalFields() {
		GelfMessage message = new GelfMessage();
		message.setJavaTimestamp(1L);
		message.addField("one", "two");
		message.addField("three", 4);
		message.addField("five", 6.0);
		message.addField("seven", 8);

		String json = message.toJson();

		Map resultingMap = (Map) JSONValue.parse(json);

		assertThat("String is string", (String) resultingMap.get("_one"), is("two"));
		assertThat("Long is long", (Long) resultingMap.get("_three"), is(4L));
		assertThat("Int is int", (Double) resultingMap.get("_five"), is(6.0));
		assertThat("Second Long is long", (Long) resultingMap.get("_seven"), is(8L));
	}
}
