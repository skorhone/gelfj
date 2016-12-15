package org.graylog2.message;

public class GelfMessageBuilderException extends RuntimeException {
	private static final long serialVersionUID = 313L;

	public GelfMessageBuilderException(String message) {
		super(message);
	}
}
