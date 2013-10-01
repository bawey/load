package ch.cern.cms.load;


/**
 * Wraps up the storage of rules between launches and exposes some related
 * functions
 * 
 */
public class RulesBase {

	private EventProcessor ep;

	protected RulesBase(ExpertController ec) {
		ep = ec.getEventProcessor();
		sampleQueryRegistrationMethod();
	}

	private void sampleQueryRegistrationMethod() {
	}
}
