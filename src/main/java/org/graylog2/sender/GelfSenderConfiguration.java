package org.graylog2.sender;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GelfSenderConfiguration {
	public static final String DEFAULT_PROTOCOL = "udp";
	public static final int DEFAULT_PORT = 12201;
	public static final int DEFAULT_RETRIES = 5;
	public static final int DEFAULT_THREADED_QUEUE_TIMEOUT = 100;
	public static final int DEFAULT_THREADED_QUEUE_MAX_DEPTH = 100;

	private URI targetURI;
	private boolean threaded;
	private int threadedQueueMaxDepth;
	private int threadedQueueTimeout;
	private int maxRetries;

	public GelfSenderConfiguration() {
		this.maxRetries = DEFAULT_RETRIES;
		this.threadedQueueTimeout = DEFAULT_THREADED_QUEUE_TIMEOUT;
		this.threadedQueueMaxDepth = DEFAULT_THREADED_QUEUE_MAX_DEPTH;
	}

	public String getTargetURI() {
		return targetURI.toString();
	}

	public void setTargetURI(String targetURI) {
		this.targetURI = toURI(targetURI);
	}

	private URI toURI(String uri) {
		int colon = uri.indexOf(':');
		if (colon == -1) {
			uri = DEFAULT_PROTOCOL + "://" + uri;
		} else if (!uri.substring(colon).startsWith("://")) {
			uri = uri.substring(0, colon + 1) + "//" + uri.substring(colon + 1);
		}
		return URI.create(uri);
	}

	public String getProtocol() {
		return targetURI.getScheme().toLowerCase();
	}

	public String getTargetHost() {
		return targetURI.getHost();
	}

	public int getTargetPort() {
		return targetURI.getPort();
	}

	public boolean isThreaded() {
		return threaded;
	}

	public void setThreaded(boolean threaded) {
		this.threaded = threaded;
	}

	public int getThreadedQueueMaxDepth() {
		return threadedQueueMaxDepth;
	}

	public void setThreadedQueueMaxDepth(int threadedQueueMaxDepth) {
		this.threadedQueueMaxDepth = threadedQueueMaxDepth;
	}

	public int getThreadedQueueTimeout() {
		return threadedQueueTimeout;
	}

	public void setThreadedQueueTimeout(int threadedQueueTimeout) {
		this.threadedQueueTimeout = threadedQueueTimeout;
	}

	public int getMaxRetries() {
		return maxRetries;
	}

	public void setMaxRetries(int maxRetries) {
		this.maxRetries = maxRetries;
	}

	public String getURIOption(String key) {
		String query = targetURI.getQuery();
		if (query != null) {
			Matcher matcher = Pattern.compile("(^|&)" + key + "=([^&]*)").matcher(query);
			if (matcher.find()) {
				return matcher.group(2);
			}
		}
		return null;
	}
	
	public int getSendTimeout() {
		String sendTimeout = getURIOption("sendTimeout");
		if (sendTimeout != null) {
			return Integer.valueOf(sendTimeout);
		}
		return 1000;
	}

	public int getSendBufferSize() {
		String sendBufferSize = getURIOption("sendBufferSize");
		if (sendBufferSize != null) {
			return Integer.valueOf(sendBufferSize);
		}
		return 0;
	}
}