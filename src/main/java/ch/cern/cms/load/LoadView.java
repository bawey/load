package ch.cern.cms.load;

import com.espertech.esper.client.StatementAwareUpdateListener;

public interface LoadView {

	public StatementAwareUpdateListener getVerboseStatementListener();
	public StatementAwareUpdateListener getWatchedStatementListener();
	
}
