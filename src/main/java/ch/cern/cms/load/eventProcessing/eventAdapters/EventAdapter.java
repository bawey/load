package ch.cern.cms.load.eventProcessing.eventAdapters;

import java.util.Map;

public interface EventAdapter {
	public boolean generateEvent(Map<String, Object> data);
}
