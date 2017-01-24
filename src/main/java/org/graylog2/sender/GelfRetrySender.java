package org.graylog2.sender;

public class GelfRetrySender implements GelfSender {
	private final GelfSender gelfSender;
	private final int maxRetries;

	public GelfRetrySender(GelfSender gelfSender, GelfSenderConfiguration configuration) {
		this.gelfSender = gelfSender;
		this.maxRetries = configuration.getMaxRetries();
	}

	@Override
	public void sendMessage(String message) throws GelfSenderException {
		Exception firstException = null;
		for (int retry = 0; retry < maxRetries; retry++) {
			try {
				gelfSender.sendMessage(message);
				return;
			} catch (Exception exception) {
				if (firstException == null) {
					firstException = exception;
				}
			}
		}
		throw new GelfSenderException(GelfSenderException.ERROR_CODE_GENERIC_ERROR, firstException);
	}

	@Override
	public void close() {
		gelfSender.close();
	}
}
