package org.graylog2.log4j2;

import java.nio.charset.Charset;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.net.Severity;
import org.graylog2.field.FieldExtractor;
import org.graylog2.field.FieldExtractors;
import org.graylog2.message.GelfMessageBuilder;
import org.graylog2.message.GelfMessageBuilderConfiguration;

@Plugin(name = "ExtGelfLayout", category = "Core", elementType = "layout", printObject = true)
public class GelfLayout extends AbstractStringLayout {
	private static final String LOGGER_NDC = "loggerNdc";
	private static final String CONTENT_TYPE = "application/json";
	private GelfMessageBuilderConfiguration gelfMessageBuilderConfiguration;
	private FieldExtractor fieldExtractor;

	protected GelfLayout(GelfMessageBuilderConfiguration gelfMessageBuilderConfiguration,
			FieldExtractor fieldExtractor) {
		super(Charset.forName("utf-8"));
		this.gelfMessageBuilderConfiguration = gelfMessageBuilderConfiguration;
		this.fieldExtractor = fieldExtractor;
	}

	@Override
	public String getContentType() {
		return CONTENT_TYPE + "; charset=" + getCharset();
	}

	@Override
	public String toSerializable(LogEvent event) {
		GelfMessageBuilder builder = new GelfMessageBuilder(gelfMessageBuilderConfiguration);
		builder.setTimestamp(event.getTimeMillis());
		String formattedMessage = event.getMessage().getFormattedMessage();
		builder.setMessage(formattedMessage);
		builder.setThrowable(event.getThrown());
		builder.setLevel(String.valueOf(Severity.getSeverity(event.getLevel()).getCode()));
		builder.addField(GelfMessageBuilder.THREAD_NAME_FIELD, event.getThreadName());
		builder.addField(GelfMessageBuilder.NATIVE_LEVEL_FIELD, event.getLevel().name());
		builder.addField(GelfMessageBuilder.LOGGER_NAME_FIELD, event.getLoggerName());
		String ndc = event.getContextStack().peek();
		if (event.isIncludeLocation()) {
			StackTraceElement source = event.getSource();
			if (source != null) {
				builder.addField(GelfMessageBuilder.CLASS_NAME_FIELD, source.getClassName());
				builder.addField(GelfMessageBuilder.METHOD_NAME_FIELD, source.getMethodName());
			}
		}
		if (fieldExtractor != null) {
			builder.addFields(fieldExtractor.getFields(event.getMessage()));
		}
		if (ndc != null) {
			builder.addField(LOGGER_NDC, ndc);
		}
		return builder.build().toJson();
	}

	@PluginFactory
	public static GelfLayout createLayout(
			@PluginAttribute(value = "extractStackTrace", defaultBoolean = true) boolean extractStacktrace,
			@PluginAttribute(value = "originHost") String originHost,
			@PluginAttribute(value = "facility") String facility,
			@PluginAttribute(value = "fieldExtractor") String fieldExtractor) {
		GelfMessageBuilderConfiguration gelfMessageBuilderConfiguration = new GelfMessageBuilderConfiguration();
		gelfMessageBuilderConfiguration.setExtractStacktrace(extractStacktrace);
		gelfMessageBuilderConfiguration.setFacility(facility);
		gelfMessageBuilderConfiguration.setOriginHost(originHost);

		FieldExtractor fieldExt = fieldExtractor != null ? FieldExtractors.getInstance(fieldExtractor)
				: FieldExtractors.getDefaultInstance();

		return new GelfLayout(gelfMessageBuilderConfiguration, fieldExt);
	}
}