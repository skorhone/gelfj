package org.graylog2.util;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Fields {
	private static Map<Class<?>, Accessor> ACCESSORS;
	private static ReadWriteLock LOCK;

	static {
		ACCESSORS = new WeakHashMap<Class<?>, Accessor>();
		LOCK = new ReentrantReadWriteLock();
	}

	public static Map<String, ? extends Object> getFields(Object provider) {
		Class<?> providerType = provider.getClass();
		Accessor accessor;
		LOCK.readLock().lock();
		try {
			accessor = ACCESSORS.get(providerType);
		} finally {
			LOCK.readLock().unlock();
		}
		if (accessor == null) {
			LOCK.writeLock().lock();
			try {
				accessor = ACCESSORS.get(providerType);
				if (accessor == null) {
					accessor = new Accessor(providerType);
					ACCESSORS.put(providerType, accessor);
				}
			} finally {
				LOCK.writeLock().unlock();
			}
		}
		return accessor.getFields(provider);
	}

	private static class Accessor {
		private final Method getMethod;

		public Accessor(Class<?> providerType) {
			Method getMethod = null;
			try {
				getMethod = providerType.getMethod("getFields");
				if (getMethod.getReturnType() == null || !Map.class.isAssignableFrom(getMethod.getReturnType())) {
					getMethod = null;
				}
			} catch (Exception exception) {
			}
			this.getMethod = getMethod;
		}

		@SuppressWarnings({ "unchecked" })
		public Map<String, ? extends Object> getFields(Object provider) {
			try {
				return (Map<String, ? extends Object>) (getMethod != null ? getMethod.invoke(provider) : null);
			} catch (Exception exception) {
			}
			return null;
		}
	}
}
