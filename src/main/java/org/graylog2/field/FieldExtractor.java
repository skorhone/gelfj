package org.graylog2.field;

import java.util.Map;

public interface FieldExtractor {
	public Map<String, ? extends Object> getFields(Object provider);
}
