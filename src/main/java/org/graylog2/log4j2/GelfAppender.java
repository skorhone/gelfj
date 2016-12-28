package org.graylog2.log4j2;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.graylog2.exception.ExceptionTracker;
import org.graylog2.sender.GelfSender;
import org.graylog2.sender.GelfSenderConfiguration;
import org.graylog2.sender.GelfSenderFactory;

@Plugin(name = "Gelf", category = "Core", elementType = "appender", printObject = true)
public class GelfAppender extends AbstractAppender {
	private GelfSenderConfiguration gelfSenderConfiguration;
	private ExceptionTracker exceptionTracker;
	private GelfSender gelfSender;

	public GelfAppender(String name, Filter filter, Layout<? extends Serializable> layout,
			GelfSenderConfiguration gelfSenderConfiguration) {
		super(name, filter, layout);
		this.gelfSenderConfiguration = gelfSenderConfiguration;
		this.exceptionTracker = new ExceptionTracker();
	}

	@Override
	public void start() {
		if (gelfSender == null) {
			gelfSender = GelfSenderFactory.getInstance().createSender(gelfSenderConfiguration);
		}
		super.start();
	}

	@Override
	public boolean stop(long timeout, TimeUnit timeUnit) {
		close();
		return super.stop(timeout, timeUnit);
	}

	@Override
	public void stop() {
		close();
		super.stop();
	}

	private void close() {
		if (gelfSender != null) {
			gelfSender.close();
			gelfSender = null;
		}
	}

	public void append(LogEvent event) {
		try {
			gelfSender.sendMessage(getLayout().toSerializable(event).toString());
		} catch (Exception exception) {
			if (!exceptionTracker.isRepeating(exception)) {
				getHandler().error("Could not send gelf message", exception);
			}
		}
	}

	@PluginBuilderFactory
	public static Builder createAppender() {
		return new Builder();
	}

	public static class Builder implements org.apache.logging.log4j.core.util.Builder<GelfAppender> {
		@PluginBuilderAttribute
		@Required(message = "A name for the GelfAppender must be specified")
		private String name;
		@PluginBuilderAttribute
		@Required(message = "A targetURI for the GelfAppender must be specified")
		private String targetURI;
		@PluginBuilderAttribute
		private boolean threaded;
		@PluginBuilderAttribute
		private int threadedQueueMaxDepth;
		@PluginBuilderAttribute
		private int threadedQueueTimeout;
		@PluginBuilderAttribute
		private int maxRetries;
		@PluginElement("Layout")
		@Required(message = "Layout for the GelfAppender must be specified")
		private Layout<? extends Serializable> layout;
		@PluginElement("Filter")
		private Filter filter;

		public Builder() {
			this.threaded = false;
			this.threadedQueueTimeout = GelfSenderConfiguration.DEFAULT_THREADED_QUEUE_TIMEOUT;
			this.threadedQueueMaxDepth = GelfSenderConfiguration.DEFAULT_THREADED_QUEUE_MAX_DEPTH;
		}

		public Builder setName(String name) {
			this.name = name;
			return this;
		}

		public Builder setTargetURI(String targetURI) {
			this.targetURI = targetURI;
			return this;
		}

		public Builder setThreaded(boolean threaded) {
			this.threaded = threaded;
			return this;
		}

		public Builder setThreadedQueueTimeout(int threadedQueueTimeout) {
			this.threadedQueueTimeout = threadedQueueTimeout;
			return this;
		}

		public Builder setThreadedQueueMaxDepth(int threadedQueueMaxDepth) {
			this.threadedQueueMaxDepth = threadedQueueMaxDepth;
			return this;
		}

		public Builder setMaxRetries(int maxRetries) {
			this.maxRetries = maxRetries;
			return this;
		}

		public Builder setFilter(Filter filter) {
			this.filter = filter;
			return this;
		}

		public Builder setLayout(Layout<? extends Serializable> layout) {
			this.layout = layout;
			return this;
		}

		public GelfAppender build() {
			GelfSenderConfiguration gelfSenderConfiguration = new GelfSenderConfiguration();
			gelfSenderConfiguration.setTargetURI(targetURI);
			gelfSenderConfiguration.setThreaded(threaded);
			gelfSenderConfiguration.setThreadedQueueMaxDepth(threadedQueueMaxDepth);
			gelfSenderConfiguration.setThreadedQueueTimeout(threadedQueueTimeout);
			gelfSenderConfiguration.setMaxRetries(maxRetries);

			return new GelfAppender(name, filter, layout, gelfSenderConfiguration);
		}
	}
}