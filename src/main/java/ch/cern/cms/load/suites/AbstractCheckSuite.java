package ch.cern.cms.load.suites;

import ch.cern.cms.load.EventProcessor;

import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.UpdateListener;

public abstract class AbstractCheckSuite {
	protected EventProcessor ep;

	public AbstractCheckSuite(EventProcessor ep) {
		this.ep = ep;
	}

	public abstract void registerLogic();

	public abstract void registerVariables();

	public abstract void registerViews();

	protected EPStatement epl(String epl) {
		return ep.epl(epl);
	}

	protected void epl(String epl, UpdateListener listener) {
		ep.epl(epl, listener);
	}

}
