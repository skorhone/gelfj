package org.graylog2.sender;

public class GelfSenderException extends Exception {
    public static final int ERROR_CODE_GENERIC_ERROR = -1;
    public static final int ERROR_CODE_SHUTTING_DOWN = 11;
    public static final int ERROR_CODE_MESSAGE_NOT_VALID = 12;
	private static final long serialVersionUID = 1L;
	private int errorCode;

	public GelfSenderException(int errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}

	public GelfSenderException(int errorCode) {
		super();
		this.errorCode = errorCode;
	}
	
	public GelfSenderException(int errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public GelfSenderException(int errorCode, Throwable cause) {
		super(cause);
		this.errorCode = errorCode;
	}
	
	@Override
	public Exception getCause() {
		return (Exception)super.getCause();
	}
	
	public int getErrorCode() {
		return errorCode;
	}
}
