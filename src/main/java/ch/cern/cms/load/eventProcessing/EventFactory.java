package ch.cern.cms.load.eventProcessing;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ch.cern.cms.load.eventProcessing.eventAdapters.EventAdapter;
import ch.cern.cms.load.eventProcessing.eventAdapters.SSCCEventAdapter;
import ch.cern.cms.load.model.Model;

/**
 * The {@link Model} holds an internal data representation. This class will be
 * used as proxy between the Model (data fetched via WS) and CEP.
 * 
 * @author Tomasz Bawej
 * 
 */
public class EventFactory {
	private static EventFactory instance;

	public static EventFactory getInstance() {
		if (instance == null) {
			synchronized (EventFactory.class) {
				if (instance == null) {
					instance = new EventFactory();
					SSCCEventAdapter.registerAll();
				}
			}
		}
		return instance;
	}

	private EventFactory() {
	}

	private List<EventAdapter> adapters = new LinkedList<EventAdapter>();

	public void registerAdapter(EventAdapter adapter) {
		this.adapters.add(adapter);
	}

	public void produceEvents(Map<String, Object> data) {
		for (EventAdapter adapter : adapters) {
			adapter.generateEvent(data);
		}
	}

}
