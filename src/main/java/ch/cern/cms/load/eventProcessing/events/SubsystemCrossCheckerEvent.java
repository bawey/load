package ch.cern.cms.load.eventProcessing.events;

import java.util.Map;

/**
 * A very simplified object representation of CC params map. Will get plugged
 * into CEP engine.
 * 
 * @author Tomasz Bawej
 * 
 */

public class SubsystemCrossCheckerEvent {

	private final String subsys;
	private final boolean clockChanged;
	private final long confTime;
	private final boolean fedsChanged;

	public SubsystemCrossCheckerEvent(String subsys, boolean clockChanged, long confTime, boolean fedsChanged) {
		super();
		this.subsys = subsys;
		this.clockChanged = clockChanged;
		this.confTime = confTime;
		this.fedsChanged = fedsChanged;
	}

	public String getSubsys() {
		return subsys;
	}

	public boolean isClockChanged() {
		return clockChanged;
	}

	public long getConfTime() {
		return confTime;
	}

	public boolean isFedsChanged() {
		return fedsChanged;
	}

	@Override
	public String toString() {
		return "SubsystemCrossCheckerEvent [subsys=" + subsys + ", clockChanged=" + clockChanged + ", confTime=" + confTime
				+ ", fedsChanged=" + fedsChanged + "]";
	}
}
