package ch.cern.cms.load.suites;

import ch.cern.cms.load.EventProcessor;

public class FedCheckSuite extends AbstractCheckSuite {

	public FedCheckSuite(EventProcessor ep) {
		super(ep);
	}

	public static final String VAR_SESSION_ID = "SESSION_ID";
	public static final String VAR_AVG_TRIGGER_RATE = "AVG_TRIGGER_RATE";

	@Override
	public void registerViews() {

	}

	@Override
	public void registerVariables() {
		epl("create variable String " + VAR_SESSION_ID + "=''");
	}

	@Override
	public void registerLogic() {
	}

}
