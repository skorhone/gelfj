package org.graylog2.field;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReflectionFieldExtractor implements FieldExtractor {
	private Map<Class<?>, Accessor> accessors;
	private ReadWriteLock lock;

	public ReflectionFieldExtractor() {
		accessors = new WeakHashMap<Class<?>, Accessor>();
		lock = new ReentrantReadWriteLock();
	}

	public Map<String, ? extends Object> getFields(Object provider) {
		if (provider == null) {
			return null;
		}
		Class<?> providerType = provider.getClass();
		Accessor accessor;
		lock.readLock().lock();
		try {
			accessor = accessors.get(providerType);
		} finally {
			lock.readLock().unlock();
		}
		if (accessor == null) {
			lock.writeLock().lock();
			try {
				accessor = accessors.get(providerType);
				if (accessor == null) {
					accessor = new Accessor(providerType);
					accessors.put(providerType, accessor);
				}
			} finally {
				lock.writeLock().unlock();
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
			} catch (Exception ignoredException) {
			}
			return null;
		}
	}
}
