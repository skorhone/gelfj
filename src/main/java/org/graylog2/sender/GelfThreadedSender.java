package org.graylog2.sender;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.graylog2.message.GelfMessage;

public class GelfThreadedSender implements GelfSender {
	enum Status {
		ACTIVE, CLOSE_WAITING, CLOSE_FORCED, CLOSED
	}

	private static final GelfMessage CLOSE_MESSAGE = new GelfMessage();
	private static final int SHUTDOWN_TIMEOUT = 10000;
	private final GelfSender sender;
	private final BlockingQueue<GelfMessage> messageQueue;
	private final int timeout;
	private Thread thread;
	private volatile Status status;

	public GelfThreadedSender(GelfSender sender, int timeout, int maxQueueDepth) {
		this.status = Status.ACTIVE;
		this.sender = sender;
		this.timeout = timeout;
		this.messageQueue = new ArrayBlockingQueue<GelfMessage>(maxQueueDepth, true);
	}

	public GelfSenderResult sendMessage(GelfMessage message) {
		if (!message.isValid()) {
			return GelfSenderResult.MESSAGE_NOT_VALID;
		}
		if (isClosed()) {
			return GelfSenderResult.MESSAGE_NOT_VALID_OR_SHUTTING_DOWN;
		}
		if (!isInitialized()) {
			initialize();
		}
		try {
			if (!messageQueue.offer(message, timeout, TimeUnit.MILLISECONDS)) {
				throw new InterruptedException("GelfThreadedSender queue is full, discardin message");
			}
		} catch (InterruptedException exception) {
			return new GelfSenderResult(GelfSenderResult.ERROR_CODE, exception);
		}
		return GelfSenderResult.OK;
	}

	private void initialize() {
		thread = new Thread(new GelfSenderThread(), "GelfSender");
		thread.start();
	}

	private boolean isInitialized() {
		return thread != null;
	}

	public boolean isClosed() {
		return status != Status.ACTIVE;
	}

	public Status getStatus() {
		return status;
	}

	public void close() {
		if (!isClosed()) {
			if (isInitialized()) {
				status = Status.CLOSE_WAITING;
				try {
					messageQueue.offer(CLOSE_MESSAGE);
					thread.join(SHUTDOWN_TIMEOUT);
				} catch (InterruptedException ignoredException) {
				}
				if (thread.isAlive()) {
					status = Status.CLOSE_FORCED;
					thread.interrupt();
				} else {
					status = Status.CLOSED;
				}
			} else {
				status = Status.CLOSED;
			}
		}
	}

	public class GelfSenderThread implements Runnable {
		private GelfMessage currentMessage;

		public void run() {
			while (isActive()) {
				if (currentMessage == null) {
					try {
						currentMessage = messageQueue.poll(1000, TimeUnit.MILLISECONDS);
					} catch (InterruptedException ignoredException) {
					}
				}
				if (currentMessage != null) {
					if (currentMessage == CLOSE_MESSAGE
							|| GelfSenderResult.OK.equals(sender.sendMessage(currentMessage))) {
						currentMessage = null;
					}
				}
			}
			sender.close();
		}

		private boolean isActive() {
			switch (getStatus()) {
			case CLOSED:
				return false;
			case CLOSE_FORCED:
				return false;
			case CLOSE_WAITING:
				return currentMessage != null || !messageQueue.isEmpty();
			default:
				return true;
			}
		}
	}
}
