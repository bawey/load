package ch.cern.cms.esper;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import ch.cern.cms.load.Load;

import com.espertech.esper.client.EPServiceProviderIsolated;
import com.espertech.esper.client.EPStatement;

public class StatementsLifecycleManager {

	private final static Logger logger = Logger.getLogger(StatementsLifecycleManager.class);

	private static StatementsLifecycleManager instance;

	private EPServiceProviderIsolated jail;
	private Map<String, EPStatement> suspended = new HashMap<String, EPStatement>();

	private StatementsLifecycleManager() {
		jail = Load.getInstance().getEventProcessor().getProvider().getEPServiceIsolated("jail");
	}

	public static StatementsLifecycleManager getInstance() {
		if (instance == null) {
			synchronized (StatementsLifecycleManager.class) {
				if (instance == null) {
					instance = new StatementsLifecycleManager();
				}
			}
		}
		return instance;
	}

	public static final void suspend(String statementName) {
		suspend(statementName, false);
	}

	public static final void suspend(String statementName, boolean reset) {
		EPStatement statement = Load.getInstance().getEventProcessor().getAdministrator().getStatement(statementName);
		boolean success = true;
		if (reset) {
			logger.warn("Great, no idea how to reset that thing. Recreate statement?");
			Load.getInstance().getEventProcessor().getRuntime()
					.sendEvent(new String[] { "Great, no idea how to reset that thing. Recreate statement?" }, "DebugMsg");
		} else {
			synchronized (StatementsLifecycleManager.class) {
				String[] jailedNames = getInstance().jail.getEPAdministrator().getStatementNames();
				for (String jailed : jailedNames) {
					if (jailed.equals(statementName)) {
						success = false;
						break;
					}
				}
				if (success) {
					instance.suspended.put(statementName, statement);
				}
			}
			if (success) {
				instance.jail.getEPAdministrator().addStatement(statement);
				Load.getInstance().getEventProcessor().getRuntime().sendEvent(new String[] { "Statement " + statementName + " suspended" }, "DebugMsg");
			} else {
				Load.getInstance().getEventProcessor().getRuntime().sendEvent(new String[] { "Statement " + statementName + " already suspended" }, "DebugMsg");
			}
		}
	}

	public static final void resume(String statementName) {
		EPStatement statement = getInstance().suspended.get(statementName);
		boolean success = false;
		synchronized (StatementsLifecycleManager.class) {
			if (instance.suspended.containsKey(statementName)) {
				instance.suspended.remove(statementName);
				success = true;
			}
		}
		if (success) {
			instance.jail.getEPAdministrator().removeStatement(statement);
			Load.getInstance().getEventProcessor().getRuntime().sendEvent(new String[] { "Statement " + statementName + " resumed" }, "DebugMsg");
		} else {
			Load.getInstance().getEventProcessor().getRuntime().sendEvent(new String[] { "Statement " + statementName + " already running" }, "DebugMsg");
		}
	}

	public static final class SubscriberSuspender {
		public void update(Map<?, ?> map) {
			System.out.println("suspending: " + map.toString());
			StatementsLifecycleManager.suspend(map.get("name").toString(), false);
		}
	}

	public static final class SubscriberResumer {
		public void update(Map<?, ?> map) {
			System.out.println("resuming: " + map.toString());
			StatementsLifecycleManager.resume(map.get("name").toString());
		}
	}

}
