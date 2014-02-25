package ch.cern.cms.load.sinks;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.StatementAwareUpdateListener;

import ch.cern.cms.load.EventSink;

/**
 * @author Tomasz Bawej
 * 
 *         This is just a dummy view to verify the concept as working.
 */

public class DummyConsolePrinter extends EventSink {

	@Override
	public StatementAwareUpdateListener getVerboseStatementListener() {
		return new StatementAwareUpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPServiceProvider epServiceProvider) {
				System.out.println(newEvents[0].getUnderlying().toString());
			}
		};
	}

	@Override
	public StatementAwareUpdateListener getWatchedStatementListener() {
		return new StatementAwareUpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPServiceProvider epServiceProvider) {
				System.out.println(newEvents[0].getUnderlying().toString());
			}
		};
	}

}
