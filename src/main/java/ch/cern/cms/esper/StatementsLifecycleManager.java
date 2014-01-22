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

	static {
		getInstance();
	}

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

	public static final String suspendAll(String... names) {
		StringBuilder sb = new StringBuilder("Suspended statements: ");
		for (String name : names) {
			EPStatement statement = Load.getInstance().getEventProcessor().getAdministrator().getStatement(name);
			sb.append(name);
			// ensures a statement is suspended
			if (!getInstance().suspend(statement)) {
				sb.append(" (suspended already)");
			}
			sb.append(name).append(", ");
		}
		sb.setLength(sb.lastIndexOf(", "));
		sb.trimToSize();
		return sb.toString();
	}

	public static final String resumeAll(String... names) {
		StringBuilder sb = new StringBuilder("Resumed statements: ");
		for (String name : names) {
			EPStatement statement = Load.getInstance().getEventProcessor().getAdministrator().getStatement(name);
			sb.append(name);
			// if (!getInstance().resume(statement)) {
			// sb.append(" (already running)");
			// }
			sb.append(name).append(", ");
		}
		sb.setLength(sb.lastIndexOf(", "));
		sb.trimToSize();
		return sb.toString();
	}

	public synchronized boolean suspend(EPStatement statement) {
		synchronized (this) {
			if (suspended.containsKey(statement.getName())) {
				return false;
			} else {
				suspended.put(statement.getName(), statement);
			}
		}
		jail.getEPAdministrator().addStatement(statement);
		return true;
	}

	public synchronized boolean resume(EPStatement statement) {
		if (suspended.containsKey(statement.getName())) {
			suspended.remove(statement.getName());
			jail.getEPAdministrator().removeStatement(statement);
			return true;
		} else {
			return false;
		}

	}
	// public static final String resume(EPStatement statement) {
	// boolean success = false;
	// // synchronized (StatementsLifecycleManager.class) {
	// // if (instance.suspended.containsKey(statementName)) {
	// // instance.suspended.remove(statementName);
	// // success = true;
	// // }
	// // }
	// // if (success) {
	// // instance.jail.getEPAdministrator().removeStatement(statement);
	// // } else {
	// // }
	// return "olaboga";
	// }
}
