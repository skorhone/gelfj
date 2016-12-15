package org.graylog2.logging;

import org.graylog2.field.FieldExtractor;

public class GelfFormatterConfiguration {
	private boolean includeLocation;
	private FieldExtractor fieldExtractor;

	public boolean isIncludeLocation() {
		return includeLocation;
	}
	
	public void setIncludeLocation(boolean includeLocation) {
		this.includeLocation = includeLocation;
	}
	
	public void setFieldExtractor(FieldExtractor fieldExtractor) {
		this.fieldExtractor = fieldExtractor;
	}
	
	public FieldExtractor getFieldExtractor() {
		return fieldExtractor;
	}
}
