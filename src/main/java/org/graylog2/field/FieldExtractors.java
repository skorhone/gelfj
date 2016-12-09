package org.graylog2.field;

public class FieldExtractors {
	public static FieldExtractor getDefaultInstance() {
		return new ReflectionFieldExtractor();
	}

	public static FieldExtractor getInstance(String type) {
		if (type == null || type.isEmpty()) {
			return null;
		}
		try {
			return FieldExtractor.class.cast(Class.forName(type).newInstance());
		} catch (Exception exception) {
			throw new IllegalStateException("Could not initialize field extractor of type " + type, exception);
		}
	}
}
