package ch.cern.cms.load.taps;

import ch.cern.cms.load.ExpertController;
import ch.cern.cms.load.eventProcessing.EventProcessor;

public interface EventsTap {
	/** adds event definitions to cofiguration **/
	public void registerEventTypes(EventProcessor eps);
	/** launches events streaming. should run in a separate thread **/
	public void openStreams(EventProcessor eps);
	/** inserts tap-related properties into expert's map **/
	public void defineProperties(ExpertController expert);
}
