package ch.cern.cms.load;

import java.util.Map;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationOperations;
import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.UpdateListener;

/**
 * a wrap-up for esper engine
 * 
 * @author Tomasz Bawej startup order:
 * 
 *         1. field types
 * 
 *         2. register events
 * 
 *         3. register rules
 */

public class EventProcessor {

	protected EventProcessor() {
		epProvider = EPServiceProviderManager.getProvider("myCEPEngine", new Configuration());
		epRT = epProvider.getEPRuntime();
		epAdmin = epProvider.getEPAdministrator();
	}

	private EPServiceProvider epProvider = null;
	private EPRuntime epRT;
	private EPAdministrator epAdmin;

	public EPStatement createEPL(String eplStatement) {
		return getAdministrator().createEPL(eplStatement);
	}

	public ConfigurationOperations getConfiguration() {
		return epAdmin.getConfiguration();
	}

	public EPServiceProvider getProvider() {
		return epProvider;
	}

	public EPRuntime getRuntime() {
		return epRT;
	}

	public EPAdministrator getAdministrator() {
		return epAdmin;
	}

	private void addEventType(Class<?> eventObjectClass) {
		System.out.println("adding event type: " + eventObjectClass.getSimpleName() + " [" + eventObjectClass.getName() + "]");
		getConfiguration().addEventType(eventObjectClass.getSimpleName(), eventObjectClass.getName());
	}

	public void registerStatement(String statement, UpdateListener listener) {
		EPStatement cepStatement = epAdmin.createEPL(statement);
		cepStatement.addListener(listener);
	}

	public EPStatement registerStatement(String statement, Object subscriber) {
		EPStatement cepStatement = epAdmin.createEPL(statement);
		cepStatement.setSubscriber(subscriber);
		return cepStatement;
	}

	public void sendEvent(Object event) {
		epRT.sendEvent(event);
	}

	public static interface MultirowSubscriber {
		public void update(Map<?, ?> data);

		public void updateStart(int insertStreamLength, int removeStreamLength);

		public void updateEnd();

	}

}
