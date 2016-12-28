package org.graylog2.exception;

import java.util.Arrays;

public class ExceptionTracker {
	private Throwable previousThrowable;
	
	public boolean isRepeating(Throwable throwable) {
		if (previousThrowable != null) {
			if (areSame(throwable, previousThrowable)) {
				return true;
			}
		}
		previousThrowable = throwable;
		return false;
	}
	
	private boolean areSame(Throwable t1, Throwable t2) {
		if (t1.getClass() != t2.getClass()) {
			return false;
		}
		String m1 = t1.getMessage();
		String m2 = t2.getMessage();
		if (m1 != null && m2 == null || m1 != null && !m1.equals(m2)) {
			return false;
		}
		return Arrays.equals(t1.getStackTrace(), t2.getStackTrace());
	}
}
