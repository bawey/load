package ch.cern.cms.load.model;

public interface ModelListener {
	public static enum EventType {
		DATA_SET_CHANGED,
		DATA_SET_UPDATED,
		NOTIFICATION_RECEIVED;
	}

	public void react(EventType changeType);
}
