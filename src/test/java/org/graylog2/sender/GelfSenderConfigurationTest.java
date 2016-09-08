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
		configuration.setGraylogURI("tcp:localhost");
		assertEquals("tcp", configuration.getProtocol());
		assertEquals("localhost", configuration.getGraylogHost());
	}

	@Test
	public void testSetTcpURIWithPort() {
		configuration.setGraylogURI("tcp:localhost:1000");
		assertEquals("tcp", configuration.getProtocol());
		assertEquals("localhost", configuration.getGraylogHost());
		assertEquals(1000, configuration.getGraylogPort());
	}

	@Test
	public void testSetUdpURI() {
		configuration.setGraylogURI("udp:localhost");
		assertEquals("udp", configuration.getProtocol());
		assertEquals("localhost", configuration.getGraylogHost());
	}

	@Test
	public void testSetUdpURIWithPort() {
		configuration.setGraylogURI("udp:localhost:1000");
		assertEquals("udp", configuration.getProtocol());
		assertEquals("localhost", configuration.getGraylogHost());
		assertEquals(1000, configuration.getGraylogPort());
	}

	@Test
	public void testSetHttpURI() {
		configuration.setGraylogURI("http://localhost:1000");
		assertEquals("http", configuration.getProtocol());
		assertEquals("http://localhost:1000", configuration.getGraylogURI());
	}

	@Test
	public void testSetHttpsURI() {
		configuration.setGraylogURI("https://localhost:1000");
		assertEquals("https", configuration.getProtocol());
		assertEquals("https://localhost:1000", configuration.getGraylogURI());
	}
}