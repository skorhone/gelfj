package org.graylog2.exception;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class ExceptionTrackerTest {
	private ExceptionTracker exceptionTracker;

	@Before
	public void setup() {
		this.exceptionTracker = new ExceptionTracker();
	}

	@Test
	public void testIsRepeatingFirst() {
		assertFalse(exceptionTracker.isRepeating(new Exception()));
	}

	@Test
	public void testIsRepeatingSame() {
		for (int i = 0; i < 2; i++) {
			assertEquals(i == 1, exceptionTracker.isRepeating(new RuntimeException()));
		}
	}

	@Test
	public void testIsRepeatingDifferent() {
		assertFalse(exceptionTracker.isRepeating(new Exception()));
		assertFalse(exceptionTracker.isRepeating(new RuntimeException()));
	}
}
