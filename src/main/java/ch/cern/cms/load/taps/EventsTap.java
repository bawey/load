package ch.cern.cms.load.taps;

import ch.cern.cms.load.EventProcessor;
import ch.cern.cms.load.ExpertController;

public interface EventsTap {
	/** adds event definitions to configuration **/
	public void registerEventTypes(EventProcessor eps);
	/** launches events streaming. should run in a separate thread **/
	public void openStreams(EventProcessor eps);
	/** inserts tap-related properties into expert's map **/
	public void defineProperties(ExpertController expert);
}
