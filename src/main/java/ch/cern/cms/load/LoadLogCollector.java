package ch.cern.cms.load;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

public class LoadLogCollector extends AppenderSkeleton {

	public static final String SETTINGS_KEY = "logSinks";

	public LoadLogCollector() {
	}

	protected void addSink(LogSink sink) {
		sinks.add(sink);
	}

	private static List<LogSink> sinks = new ArrayList<LogSink>();

	/**
	 * @see org.apache.log4j.AppenderSkeleton#requiresLayout()
	 */
	public boolean requiresLayout() {
		return true;
	}

	/**
	 * @see org.apache.log4j.AppenderSkeleton#append(LoggingEvent)
	 */
	protected void append(LoggingEvent event) {
		String text = super.getLayout().format(event);
		if (event.getThrowableInformation() != null)
			System.out.println(event.getThrowableInformation().toString());
		for (LogSink sink : sinks) {
			if (sink.log(event)) {
				continue;
			}
			if (sink.log(text)) {
				continue;
			}
		}
	}

	/**
	 * @see org.apache.log4j.AppenderSkeleton#close()
	 */
	public void close() {
		super.closed = true;
	}

	public static void registerSink(LogSink sink) {
		sinks.add(sink);
	}

}