package org.graylog2.sender;

import java.io.IOException;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class GelfSenderFactory {
	private GelfSenderFactory() {
	}

	public static GelfSenderFactory getInstance() {
		return new GelfSenderFactory();
	}

	public GelfSender createSender(GelfSenderConfiguration configuration) {
		GelfSender gelfSender = null;
		if (configuration.getTargetURI() == null) {
			throw new GelfSenderConfigurationException("Target uri is empty!");
		}
		try {
			if ("tcp".equals(configuration.getProtocol())) {
				gelfSender = new GelfTCPSender(configuration);
			} else if ("udp".equals(configuration.getProtocol())) {
				gelfSender = new GelfUDPSender(configuration, !configuration.isThreaded());
			} else if ("amqp".equals(configuration.getProtocol())) {
				gelfSender = new GelfAMQPSender(configuration, !configuration.isThreaded());
			} else if ("http".equals(configuration.getProtocol()) || "https".equals(configuration.getProtocol())) {
				gelfSender = new GelfHTTPSender(configuration);
			} else {
				throw new GelfSenderConfigurationException("Unsupported protocol: " + configuration.getProtocol());
			}
			if (configuration.isThreaded()) {
				gelfSender = new GelfThreadedSender(gelfSender, configuration);
			}
			return gelfSender;
		} catch (UnknownHostException e) {
			throw new GelfSenderConfigurationException("Unknown Graylog2 hostname:" + configuration.getTargetHost(), e);
		} catch (SocketException e) {
			throw new GelfSenderConfigurationException("Socket exception", e);
		} catch (IOException e) {
			throw new GelfSenderConfigurationException("IO exception", e);
		} catch (URISyntaxException e) {
			throw new GelfSenderConfigurationException("URI exception", e);
		} catch (NoSuchAlgorithmException e) {
			throw new GelfSenderConfigurationException("Algorithm exception", e);
		} catch (KeyManagementException e) {
			throw new GelfSenderConfigurationException("Key exception", e);
		} catch (Exception e) {
			throw new GelfSenderConfigurationException("Unknown exception while configuring GelfSender", e);
		}
	}
}
