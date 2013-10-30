package ch.cern.cms.load.eventProcessing.eventAdapters;

import java.util.Map;

import ch.cern.cms.load.EventProcessor;
import ch.cern.cms.load.Load;
import ch.cern.cms.load.eventProcessing.EventFactory;
import ch.cern.cms.load.eventProcessing.events.SubsystemCrossCheckerEvent;

public class SSCCEventAdapter implements EventAdapter {

	public static void registerAll() {
		System.out.println("Are u alive?");
		for (String subsystem : new String[] { "DAQ", "TRG", "ECAL", "PIXEL", "TRACKER", "DCS" }) {
			EventFactory.getInstance().registerAdapter(new SSCCEventAdapter(subsystem));
		}
	}

	private final String key;
	private final String subsys;

	// adapters create themselves and register with the factory
	private SSCCEventAdapter(String subsys) {
		this.subsys = subsys;
		this.key = "JSON_" + subsys + "_CC_PARAM_MAP";
	}

	public boolean generateEvent(Map<String, Object> data) {
		if (data.containsKey(key)) {
			Map<?, ?> src = (Map<?, ?>) data.get(key);

			boolean clockChanged = Boolean.parseBoolean(src.get("CC_CLOCK_CHANGED").toString());
			Long confTime = Long.parseLong(src.get("CC_CONF_TIME").toString());
			boolean fedsChanged = Boolean.parseBoolean(src.get("CC_FEDS_CHANGED").toString());

			SubsystemCrossCheckerEvent event = new SubsystemCrossCheckerEvent(subsys, clockChanged, confTime, fedsChanged);
			System.out.println("event created!");
			// now the event should be sent to CEP
			Load.getInstance().getEventProcessor().sendEvent(event);
			
			return true;
		}
		// System.out.println("Param not found: " + key);
		return false;
	}

}
