package ch.cern.cms.load.eventProcessing;

import ch.cern.cms.load.eventData.EventProcessorStatus;
import ch.cern.cms.load.eventProcessing.events.SubsystemCrossCheckerEvent;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

/**
 * a wrap-up for esper engine
 * 
 * @author Tomasz Bawej
 * 
 */

public class EventProcessor {
	private static EventProcessor instance;

	public static EventProcessor getInstance() {
		if (instance == null) {
			synchronized (EventProcessor.class) {
				if (instance == null) {
					instance = new EventProcessor();
				}
			}
		}
		return instance;
	}

	private EventProcessor() {
		epConfig = new Configuration();
		addEventType(SubsystemCrossCheckerEvent.class);
		addEventType(EventProcessorStatus.class);
		epProvider = EPServiceProviderManager.getProvider("myCEPEngine", epConfig);
		epRT = epProvider.getEPRuntime();
		epAdmin = epProvider.getEPAdministrator();
	}

	private Configuration epConfig = null;
	private EPServiceProvider epProvider = null;
	private EPRuntime epRT;
	private EPAdministrator epAdmin;

	private void addEventType(Class<?> eventObjectClass) {
		System.out.println("adding event type: " + eventObjectClass.getSimpleName() + " [" + eventObjectClass.getName() + "]");
		epConfig.addEventType(eventObjectClass.getSimpleName(), eventObjectClass.getName());
	}

	public void registerStatement(String statement, UpdateListener listener) {
		EPStatement cepStatement = epAdmin.createEPL(statement);
		cepStatement.addListener(listener);
	}

	public void sendEvent(Object event) {
		epRT.sendEvent(event);
	}
}
