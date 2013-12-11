package ch.cern.cms.load.views;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.StatementAwareUpdateListener;

import ch.cern.cms.load.LoadView;

/**
 * @author Tomasz Bawej
 * 
 *         This is just a dummy view to verify the concept as working.
 */

public class DummyConsolePrinter extends LoadView {

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
