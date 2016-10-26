package org.graylog2.sender;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class GelfSenderConfigurationTest {
	private GelfSenderConfiguration configuration;

	@Before
	public void setUp() {
		configuration = new GelfSenderConfiguration();
	}

	@Test
	public void testSetTcpURI() {
		configuration.setTargetURI("tcp:localhost");
		assertEquals("tcp", configuration.getProtocol());
		assertEquals("localhost", configuration.getTargetHost());
	}

	@Test
	public void testSetTcpURIWithPort() {
		configuration.setTargetURI("tcp:localhost:1000");
		assertEquals("tcp", configuration.getProtocol());
		assertEquals("localhost", configuration.getTargetHost());
		assertEquals(1000, configuration.getTargetPort());
	}

	@Test
	public void testSetUdpURI() {
		configuration.setTargetURI("udp:localhost");
		assertEquals("udp", configuration.getProtocol());
		assertEquals("localhost", configuration.getTargetHost());
	}

	@Test
	public void testSetUdpURIWithPort() {
		configuration.setTargetURI("udp:localhost:1000");
		assertEquals("udp", configuration.getProtocol());
		assertEquals("localhost", configuration.getTargetHost());
		assertEquals(1000, configuration.getTargetPort());
	}

	@Test
	public void testSetHttpURI() {
		configuration.setTargetURI("http://localhost:1000");
		assertEquals("http", configuration.getProtocol());
		assertEquals("http://localhost:1000", configuration.getTargetURI());
	}

	@Test
	public void testSetHttpsURI() {
		configuration.setTargetURI("https://localhost:1000");
		assertEquals("https", configuration.getProtocol());
		assertEquals("https://localhost:1000", configuration.getTargetURI());
	}

	@Test
	public void testSetAMQP() {
		configuration
				.setTargetURI("amqp://userName:password@hostName:portNumber/virtualHost?exchange=foo&routingKey=bar");
		assertEquals("amqp", configuration.getProtocol());
		assertEquals("foo", configuration.getURIOption("exchange"));
		assertEquals("bar", configuration.getURIOption("routingKey"));
	}
}