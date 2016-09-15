package org.graylog2.sender;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.security.SecureRandom;
import java.util.Random;

import org.graylog2.message.GelfMessage;

public class GelfUDPSender implements GelfSender {
	private String destinationHost;
	private int destinationPort;
	private int sendTimeout;
	private int sendBufferSize;
	private int maxRetries;
	private UDPBufferManager bufferManager;
	private SocketAddress destinationAddress;
	private Selector selector;
	private DatagramChannel channel;
	private boolean shutdown;

	public GelfUDPSender(GelfSenderConfiguration configuration, boolean enableRetry) {
		this.destinationHost = configuration.getGraylogHost();
		this.destinationPort = configuration.getGraylogPort();
		this.sendBufferSize = configuration.getSocketSendBufferSize();
		this.sendTimeout = configuration.getSendTimeout();
		this.maxRetries = enableRetry ? configuration.getMaxRetries() : 0;
		this.bufferManager = new UDPBufferManager();
	}

	public void sendMessage(GelfMessage message) throws GelfSenderException {
		int tries = 0;
		Exception lastException = null;
		ByteBuffer[] datagrams = bufferManager.getUDPBuffers(message.toJson());
		do {
			try {
				if (!isConnected()) {
					connect();
				}
				for (ByteBuffer datagram : datagrams) {
					sendDatagram(datagram);
				}
				return;
			} catch (Exception exception) {
				tries++;
				lastException = exception;
				closeConnection();
			}
		} while (tries <= maxRetries);

		throw new GelfSenderException(GelfSenderException.ERROR_CODE_GENERIC_ERROR, lastException);
	}

	private void sendDatagram(ByteBuffer buffer) throws IOException, InterruptedException {
		while (channel.write(buffer) == 0) {
			if (selector.select(sendTimeout) == 0) {
				throw new IOException("Send operation timed out");
			}
		}
	}

	private synchronized void connect() throws IOException, GelfSenderException {
		if (shutdown) {
			throw new GelfSenderException(GelfSenderException.ERROR_CODE_SHUTTING_DOWN);
		}
		destinationAddress = new InetSocketAddress(this.destinationHost, this.destinationPort);
		if (selector == null || !selector.isOpen()) {
			selector = Selector.open();
		}
		channel = DatagramChannel.open();
		if (sendBufferSize > 0) {
			channel.setOption(StandardSocketOptions.SO_SNDBUF, sendBufferSize);
		}
		channel.configureBlocking(false);
		channel.connect(destinationAddress);
		channel.register(selector, SelectionKey.OP_WRITE);
	}

	public synchronized void close() {
		if (!shutdown) {
			shutdown = true;
			closeConnection();
		}
	}

	private void closeConnection() {
		if (channel != null) {
			try {
				channel.close();
			} catch (Exception ignoredException) {
			}
		}
		if (selector != null) {
			try {
				selector.close();
			} catch (Exception ignoredException) {
			}
		}
		channel = null;
		selector = null;
	}

	public boolean isConnected() {
		return channel != null && channel.isConnected();
	}

	/**
	 * For efficiency reasons (single shared buffer instance) this class is not
	 * thread safe!
	 */
	public static class UDPBufferManager extends AbstractBufferManager {
		private static final byte[] GELF_CHUNKED_ID = new byte[] { 0x1e, 0x0f };
		private static final int HEADER_SIZE = 2 + 8 + 2;
		private static final int MAXIMUM_CHUNK_SIZE = 1420 - HEADER_SIZE;
		private ByteBuffer sharedBuffer;
		private Random random;
		private byte[] hostId;

		public UDPBufferManager() {
			this.random = new Random();
			this.hostId = createHostId();
			this.sharedBuffer = ByteBuffer.allocateDirect(128 * 1000);
		}

		public ByteBuffer[] getUDPBuffers(String message) {
			byte[] messageBytes = getMessageAsBytes(message);
			if (messageBytes.length > MAXIMUM_CHUNK_SIZE) {
				return createMultipleDatagrams(messageBytes);
			}
			return createSingleDatagram(messageBytes);
		}

		private ByteBuffer allocateBuffer(int size) {
			if (size > sharedBuffer.capacity()) {
				return ByteBuffer.allocate(size);
			}
			return sharedBuffer.duplicate();
		}

		private ByteBuffer[] createSingleDatagram(byte[] messageBytes) {
			ByteBuffer[] datagrams = new ByteBuffer[1];
			datagrams[0] = ByteBuffer.wrap(messageBytes);
			return datagrams;
		}

		private ByteBuffer[] createMultipleDatagrams(byte[] messageBytes) {
			ByteBuffer[] datagrams = new ByteBuffer[countRequiredDatagrams(messageBytes)];

			byte[] messageId = createMessageId();

			ByteBuffer datagramBuffer = allocateBuffer(datagrams.length * HEADER_SIZE + messageBytes.length);

			for (int currentDatagramIdx = 0; currentDatagramIdx < datagrams.length; currentDatagramIdx++) {
				datagramBuffer.put(GELF_CHUNKED_ID);
				datagramBuffer.put(messageId);
				datagramBuffer.put((byte) currentDatagramIdx);
				datagramBuffer.put((byte) datagrams.length);

				int from = currentDatagramIdx * MAXIMUM_CHUNK_SIZE;
				int to = from + MAXIMUM_CHUNK_SIZE;
				if (to > messageBytes.length) {
					to = messageBytes.length;
				}
				datagramBuffer.put(messageBytes, from, to - from);

				ByteBuffer datagram = datagramBuffer;
				datagramBuffer = datagramBuffer.slice();
				datagram.flip();
				datagrams[currentDatagramIdx] = datagram;
			}
			return datagrams;
		}

		private int countRequiredDatagrams(byte[] messageBytes) {
			return (messageBytes.length + (MAXIMUM_CHUNK_SIZE - 1)) / MAXIMUM_CHUNK_SIZE;
		}

		private byte[] createMessageId() {
			byte[] messageId = new byte[8];
			byte[] randomBytes = createHostId();
			random.nextBytes(randomBytes);
			System.arraycopy(hostId, 0, messageId, 0, 4);
			System.arraycopy(randomBytes, 0, messageId, 4, 4);
			return messageId;
		}

		private byte[] createHostId() {
			byte[] hostId = new byte[4];
			byte[] address = getAddress();
			hostId[0] = (byte) new SecureRandom().nextInt();
			hostId[1] = address[0];
			hostId[2] = address[1];
			hostId[3] = address[2];
			return hostId;
		}

		private byte[] getAddress() {
			byte[] address = null;
			try {
				InetAddress inetAddress = InetAddress.getLocalHost();
				if (inetAddress != null && !inetAddress.isLoopbackAddress()) {
					address = inetAddress.getAddress();
				}
			} catch (Exception exception) {
			}
			if (address == null) {
				address = new byte[4];
				new SecureRandom().nextBytes(address);
			}
			return address;
		}
	}
}
