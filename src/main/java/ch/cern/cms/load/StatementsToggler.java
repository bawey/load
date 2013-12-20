package ch.cern.cms.load;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.espertech.esper.client.EPServiceProviderIsolated;
import com.espertech.esper.client.EPStatement;

public class StatementsToggler {
	private static final Logger logger = Logger.getLogger(StatementsToggler.class);
	private String lastState = null;
	private EPServiceProviderIsolated sandbox;

	private static final String[] states = { "running", "stopping", "configured", "configuring", "starting", "standby", "initializing", "halted",
			"initialized", "halting", "faulty", "connecting", "paused", "error", "pausing", "connected", "", " " };

	private static Map<String, List<EPStatement>> enablers = new HashMap<String, List<EPStatement>>() {
		private static final long serialVersionUID = 1L;

		{
			for (String state : states) {
				put(state.toLowerCase(), new ArrayList<EPStatement>());
			}
		}
	};
	private static Map<String, List<EPStatement>> disablers = new HashMap<String, List<EPStatement>>() {
		private static final long serialVersionUID = 1L;

		{
			for (String state : states) {
				put(state.toLowerCase(), new ArrayList<EPStatement>());
			}
		}
	};

	public StatementsToggler() {
		sandbox = Load.getInstance().getEventProcessor().getProvider().getEPServiceIsolated("sandbox");
	}

	public static void register(EPStatement statement, String[] inStates) {
		Set<String> enabled = new HashSet<String>();
		for (String inState : inStates) {
			enabled.add(inState.toLowerCase());
		}
		for (String state : states) {
			if (enabled.contains(state)) {
				enablers.get(state).add(statement);
			} else {
				disablers.get(state).add(statement);
			}
		}
	}

	public void handleStateChange(String state) {
		if (enablers.get(state.toLowerCase()) == null) {
			logger.warn("State " + state + " has no associated enablers list");
			logger.warn("Enablers contain keys: " + enablers.keySet());
			return;
		}

		for (EPStatement statement : enablers.get(state.toLowerCase())) {
			if (!Arrays.asList(sandbox.getEPAdministrator().getStatementNames()).contains(statement.getName())) {
				//sandbox.getEPAdministrator().addStatement(statement);
			}
		}
		for (EPStatement statement : disablers.get(state.toLowerCase())) {
			if (Arrays.asList(sandbox.getEPAdministrator().getStatementNames()).contains(statement.getName())) {
				//sandbox.getEPAdministrator().removeStatement(statement);
			}
		}
	}

	public void update(String state) {
		if (lastState == null || !lastState.equals(state)) {
			handleStateChange(state);
		}
		lastState = state;
	}

}
