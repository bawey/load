package ch.cern.cms.load;

import org.apache.log4j.spi.LoggingEvent;

public interface LogSink {
	public void log(String message);

	public void log(LoggingEvent loggingEvent);
}
