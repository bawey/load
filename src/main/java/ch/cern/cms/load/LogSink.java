package ch.cern.cms.load;

import org.apache.log4j.spi.LoggingEvent;

public interface LogSink {
	/**
	 * 
	 * @param message
	 * @return whether the message was processed or not
	 */
	public boolean log(String message);

	/**
	 * 
	 * @param loggingEvent
	 * @return whethet the event was processed or not
	 */
	public boolean log(LoggingEvent loggingEvent);
}
