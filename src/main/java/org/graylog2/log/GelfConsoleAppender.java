package org.graylog2.log;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.ErrorCode;
import org.apache.log4j.spi.LoggingEvent;
import org.graylog2.message.GelfMessage;
import org.graylog2.message.GelfMessageBuilderConfiguration;
import org.graylog2.message.GelfMessageBuilderException;

public class GelfConsoleAppender extends ConsoleAppender implements GelfMessageProvider {
	private GelfMessageBuilderConfiguration gelfMessageBuilderConfiguration;
	private GelfMessageFactory messageFactory;
	private boolean extractStacktrace;
	private boolean addExtendedInformation;
	private boolean includeLocation = true;

	public GelfConsoleAppender() {
		super();
		this.gelfMessageBuilderConfiguration = new GelfMessageBuilderConfiguration();
		this.messageFactory = new GelfMessageFactory();
	}

	public GelfConsoleAppender(Layout layout) {
		super(layout);
		gelfMessageBuilderConfiguration = new GelfMessageBuilderConfiguration();
	}

	public GelfConsoleAppender(Layout layout, String target) {
		super(layout, target);
		gelfMessageBuilderConfiguration = new GelfMessageBuilderConfiguration();
	}

	public boolean isExtractStacktrace() {
		return extractStacktrace;
	}

	public void setExtractStacktrace(boolean extractStacktrace) {
		this.extractStacktrace = extractStacktrace;
	}

	public boolean isAddExtendedInformation() {
		return addExtendedInformation;
	}

	public void setAddExtendedInformation(boolean addExtendedInformation) {
		this.addExtendedInformation = addExtendedInformation;
	}

	public boolean isIncludeLocation() {
		return this.includeLocation;
	}

	public void setIncludeLocation(boolean includeLocation) {
		this.includeLocation = includeLocation;
	}

	public String getOriginHost() {
		return gelfMessageBuilderConfiguration.getOriginHost();
	}

	public String getFacility() {
		return gelfMessageBuilderConfiguration.getFacility();
	}

	public GelfMessageBuilderConfiguration getGelfMessageBuilderConfiguration() {
		return gelfMessageBuilderConfiguration;
	}

	@Override
	protected void subAppend(LoggingEvent event) {
		try {
			GelfMessage gelf = messageFactory.makeMessage(layout, event, this);
			this.qw.write(gelf.toJson());
			this.qw.write(Layout.LINE_SEP);
			if (this.immediateFlush) {
				this.qw.flush();
			}
		} catch (GelfMessageBuilderException exception) {
			errorHandler.error("Error building GELF message", exception, ErrorCode.WRITE_FAILURE);
		}
	}
}
