package ch.cern.cms.load.eventData;

import java.util.Date;
import java.util.List;

public class EventProcessorStatus {
	private final long nbAccepted;
	private final List<Integer> epMacroStateInt;
	private final int age;
	private final int lid;
	private final int instance;
	private final int runNumber;
	private final String stateName;
	private final Date timestamp;
	private final long updateTime;
	private final String context;
	private final List<String> epMacroStateStr;
	private final long nbProcessed;
	private final List<Integer> epMicroStateInt;
	private final int sessionId;

	public EventProcessorStatus(long nbAccepted, List<Integer> epMacroStateInt, int age, int lid, int instance, int runNumber,
			String stateName, Date timestamp, long updateTime, String context, List<String> epMacroStateStr, long nbProcessed,
			List<Integer> epMicroStateInt, int sessionId) {
		super();
		this.nbAccepted = nbAccepted;
		this.epMacroStateInt = epMacroStateInt;
		this.age = age;
		this.lid = lid;
		this.instance = instance;
		this.runNumber = runNumber;
		this.stateName = stateName;
		this.timestamp = timestamp;
		this.updateTime = updateTime;
		this.context = context;
		this.epMacroStateStr = epMacroStateStr;
		this.nbProcessed = nbProcessed;
		this.epMicroStateInt = epMicroStateInt;
		this.sessionId = sessionId;
	}

	public long getNbAccepted() {
		return nbAccepted;
	}

	public List<Integer> getEpMacroStateInt() {
		return epMacroStateInt;
	}

	public int getAge() {
		return age;
	}

	public int getLid() {
		return lid;
	}

	public int getInstance() {
		return instance;
	}

	public int getRunNumber() {
		return runNumber;
	}

	public String getStateName() {
		return stateName;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public long getUpdateTime() {
		return updateTime;
	}

	public String getContext() {
		return context;
	}

	public List<String> getEpMacroStateStr() {
		return epMacroStateStr;
	}

	public long getNbProcessed() {
		return nbProcessed;
	}

	public List<Integer> getEpMicroStateInt() {
		return epMicroStateInt;
	}

	public int getSessionId() {
		return sessionId;
	}

	@Override
	public String toString() {
		return "EventProcessorStatus [nbAccepted=" + nbAccepted + ", epMacroStateInt=" + epMacroStateInt + ", age=" + age + ", lid=" + lid
				+ ", instance=" + instance + ", runNumber=" + runNumber + ", stateName=" + stateName + ", timestamp=" + timestamp
				+ ", updateTime=" + updateTime + ", context=" + context + ", epMacroStateStr=" + epMacroStateStr + ", nbProcessed="
				+ nbProcessed + ", epMicroStateInt=" + epMicroStateInt + ", sessionId=" + sessionId + "]";
	}
	
}
