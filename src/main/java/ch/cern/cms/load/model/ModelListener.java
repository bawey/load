package ch.cern.cms.load.model;

import java.util.Collection;

public interface ModelListener {
	public static enum EventType {
		INSERTED_PARAMS,
		CHANGED_PARAMS,
		REMOVED_PARAMS;
	}

	public static final class UpdateInfo {
		public final Collection<String> insertedParams;
		public final Collection<String> changedParams;
		public final Collection<String> removedParams;

		public UpdateInfo(Collection<String> insertedParams, Collection<String> changedParams, Collection<String> removedParams) {
			super();
			this.insertedParams = insertedParams;
			this.changedParams = changedParams;
			this.removedParams = removedParams;
		}

		// public boolean isPing(){
		// return (insertedParams == null || insertedParams.isEmpty())
		// }

	}

	public void react(EventType changeType);
}
