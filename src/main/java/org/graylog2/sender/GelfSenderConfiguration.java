package org.graylog2.sender;

public class GelfSenderConfiguration {
	private String protocol;
	private String graylogHost;
	private int graylogPort;
	private String amqpURI;
	private String amqpExchangeName;
	private String amqpRoutingKey;
	private boolean tcpKeepalive;
	private boolean threaded;
	private int threadedQueueTimeout;
	private int threadedQueueMaxDepth;
	private int socketSendBufferSize;
	private int amqpMaxRetries;

	public GelfSenderConfiguration() {
		this.protocol = "udp";
		this.graylogPort = 12201;
		this.threadedQueueMaxDepth = 1000;
		this.threadedQueueTimeout = 1000;
	}

	public String getGraylogURI() {
		return protocol + ":" + graylogHost + ":" + graylogPort;
	}

	public void setGraylogURI(String graylogURI) {
		if (graylogURI != null) {
			String[] parts = graylogURI.split(":");
			if (parts.length == 0 || parts.length > 3) {
				throw new IllegalArgumentException("Unsupported URI format: " + graylogURI);
			}
			if (parts.length == 1) {
				setGraylogHost(parts[0]);
			} else if (parts.length == 2) {
				setProtocol(parts[0]);
				setGraylogHost(parts[1]);
			} else if (parts.length == 3) {
				setProtocol(parts[0]);
				setGraylogHost(parts[1]);
				setGraylogPort(Integer.valueOf(parts[2]));
			}
		}
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getGraylogHost() {
		return graylogHost;
	}

	public void setGraylogHost(String graylogHost) {
		this.graylogHost = graylogHost;
	}

	public int getGraylogPort() {
		return graylogPort;
	}

	public void setGraylogPort(int graylogPort) {
		this.graylogPort = graylogPort;
	}

	public String getAmqpURI() {
		return amqpURI;
	}

	public void setAmqpURI(String amqpURI) {
		if (amqpURI != null) {
			setProtocol("amqp");
			this.amqpURI = amqpURI;
		}
	}

	public String getAmqpExchangeName() {
		return amqpExchangeName;
	}

	public void setAmqpExchangeName(String amqpExchangeName) {
		this.amqpExchangeName = amqpExchangeName;
	}

	public String getAmqpRoutingKey() {
		return amqpRoutingKey;
	}

	public void setAmqpRoutingKey(String amqpRoutingKey) {
		this.amqpRoutingKey = amqpRoutingKey;
	}

	public int getSocketSendBufferSize() {
		return socketSendBufferSize;
	}

	public void setSocketSendBufferSize(int socketSendBufferSize) {
		this.socketSendBufferSize = socketSendBufferSize;
	}

	public boolean isTcpKeepalive() {
		return tcpKeepalive;
	}

	public void setTcpKeepalive(boolean tcpKeepalive) {
		this.tcpKeepalive = tcpKeepalive;
	}

	public boolean isThreaded() {
		return threaded;
	}

	public void setThreaded(boolean threaded) {
		this.threaded = threaded;
	}

	public int getThreadedQueueTimeout() {
		return threadedQueueTimeout;
	}

	public void setThreadedQueueTimeout(int threadedQueueTimeout) {
		this.threadedQueueTimeout = threadedQueueTimeout;
	}

	public int getThreadedQueueMaxDepth() {
		return threadedQueueMaxDepth;
	}

	public void setThreadedQueueMaxDepth(int threadedQueueMaxDepth) {
		this.threadedQueueMaxDepth = threadedQueueMaxDepth;
	}

	public int getAmqpMaxRetries() {
		return amqpMaxRetries;
	}

	public void setAmqpMaxRetries(int amqpMaxRetries) {
		this.amqpMaxRetries = amqpMaxRetries;
	}
}