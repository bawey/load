package ch.cern.cms.esper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import ch.cern.cms.load.Load;

import com.espertech.esper.client.EPServiceProviderIsolated;
import com.espertech.esper.client.EPStatement;

public class StatementsLifecycleManager {

	private final static Logger logger = Logger.getLogger(StatementsLifecycleManager.class);

	private static StatementsLifecycleManager instance;

	private EPServiceProviderIsolated jail;
	private Map<String, EPStatement> suspended = new HashMap<String, EPStatement>();

	private BlockingQueue<EPStatement> statementsQueue = new ArrayBlockingQueue<EPStatement>(10);

	static {
		getInstance();
	}

	private StatementsLifecycleManager() {
		jail = Load.getInstance().getEventProcessor().getProvider().getEPServiceIsolated("jail");
		new Thread(statementToggler).start();
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
			if (statement == null) {
				logger.warn("Statement '" + name + "' not found!");
				sb.append(name).append(" NOT FOUND!, ");
				continue;
			}
			sb.append(name);
			// ensures a statement is suspended
			if (!getInstance().suspend(statement)) {
				sb.append(" (suspended already)");
			}
			sb.append(", ");
		}
		sb.setLength(sb.lastIndexOf(", "));
		sb.trimToSize();
		return sb.toString();
	}

	public static final String resumeAll(String... names) {
		StringBuilder sb = new StringBuilder("Resumed statements: ");
		for (String name : names) {
			EPStatement statement = Load.getInstance().getEventProcessor().getAdministrator().getStatement(name);
			if (statement == null) {
				logger.warn("Statement '" + name + "' not found!");
				sb.append(name).append(" NOT FOUND!, ");
				continue;
			}
			sb.append(name);
			if (!getInstance().resume(statement)) {
				sb.append(" (already running)");
			}
			sb.append(", ");
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
			logger.debug("About to add to statementsQueue. Queue remaining capacity: " + statementsQueue.remainingCapacity());
			statementsQueue.add(statement);
		}
		return true;
	}

	public synchronized boolean resume(EPStatement statement) {
		if (suspended.containsKey(statement.getName())) {
			suspended.remove(statement.getName());
			// logger.debug("About to add to statementsQueue. Queue remaining capacity: " + statementsQueue.remainingCapacity());
			statementsQueue.add(statement);
			return true;
		} else {
			return false;
		}

	}

	/** everything locks up when performed in the same thread **/
	private Runnable statementToggler = new Runnable() {

		private Set<String> jailedNames = new HashSet<String>();

		@Override
		public void run() {
			while (true) {
				try {
					EPStatement statement = statementsQueue.take();
					if (jailedNames.contains(statement.getName())) {
						jail.getEPAdministrator().removeStatement(statement);
						jailedNames.remove(statement.getName());
						logger.debug("Effectively resumed: " + statement.getName() + ". Statements in queue: " + statementsQueue.size() + ", remaining slots: "
								+ statementsQueue.remainingCapacity());
					} else {
						jail.getEPAdministrator().addStatement(statement);
						jailedNames.add(statement.getName());
						logger.debug("Effectively suspended: " + statement.getName());
					}
				} catch (InterruptedException e) {
					logger.warn("Some problem while retrieving statement for suspension", e);
				}
			}
		}
	};

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
