package ch.cern.cms.load.taps;

import ch.cern.cms.load.EventProcessor;
import ch.cern.cms.load.ExpertController;

public abstract class EventsTap {

	protected EventsTap() {

	}

	protected EventsTap(ExpertController expert) {
		initWithExpert(expert);
	}

	protected void initWithExpert(ExpertController expert) {
		registerEventTypes(expert.getEventProcessor());
	}

	/** adds event definitions to configuration **/
	public abstract void registerEventTypes(EventProcessor eps);

	/** launches events streaming. should run in a separate thread **/
	public abstract void openStreams(EventProcessor eps);

	/**
	 * allows setting things up before performing the full registration with
	 * expert {@link EventsTap#initWithExpert(ExpertController)}
	 **/
	public abstract void setUp(ExpertController expert);
}
