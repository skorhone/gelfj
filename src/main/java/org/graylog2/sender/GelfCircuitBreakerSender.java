package org.graylog2.sender;

import java.util.concurrent.TimeUnit;

public class GelfCircuitBreakerSender implements GelfSender {
	private final GelfSender gelfSender;
	private final long reenableTimeout;
	private final int errorCountThreshold;
	private int errorCount;
	private boolean halfOpen;
	private long lastErrorTime;

	public GelfCircuitBreakerSender(GelfSender gelfSender, GelfSenderConfiguration configuration) {
		this.gelfSender = gelfSender;
		this.reenableTimeout = TimeUnit.MILLISECONDS.toNanos(configuration.getReenableTimeout());
		this.errorCountThreshold = configuration.getReenableTimeout();
		this.errorCount = 0;
		this.halfOpen = false;
	}

	@Override
	public void sendMessage(String message) throws GelfSenderException {
		startSend();
		try {
			gelfSender.sendMessage(message);
			endSend();
		} catch (GelfSenderException exception) {
			endSendWithError();
			throw exception;
		} catch (RuntimeException exception) {
			endSendWithError();
			throw exception;
		}
	}

	private synchronized void startSend() throws GelfSenderException {
		if (errorCount > errorCountThreshold) {
			if (halfOpen || System.nanoTime() - lastErrorTime < reenableTimeout) {
				throw new GelfSenderException(GelfSenderException.ERROR_CODE_GENERIC_ERROR,
						"Gelf sender is temporarily disable due to excessive amount of send errors");
			}
			halfOpen = true;
		}
	}

	private synchronized void endSend() {
		errorCount = 0;
		halfOpen = false;
	}

	private synchronized void endSendWithError() {
		errorCount++;
		lastErrorTime = System.nanoTime();
		halfOpen = false;
	}

	@Override
	public void close() {
		gelfSender.close();
	}
}
