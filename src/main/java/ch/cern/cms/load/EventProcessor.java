package ch.cern.cms.load;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.apache.log4j.Logger;

import ch.cern.cms.esper.annotations.Verbose;
import ch.cern.cms.load.hwdb.CmsHw;
import ch.cern.cms.load.hwdb.HwInfo;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationOperations;
import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPStatementStateListener;
import com.espertech.esper.client.EventBean;
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

	private final static Logger logger = Logger.getLogger(EventProcessor.class);

	protected EventProcessor() {
		Configuration c = new Configuration();
		// c.getEngineDefaults().getThreading().setInternalTimerEnabled(false);
		c.getEngineDefaults().getThreading().setListenerDispatchPreserveOrder(true);
		c.addImport(HwInfo.class);
		c.addImport(CmsHw.class);
		c.addImport(Verbose.class);

		epProvider = EPServiceProviderManager.getProvider("myCEPEngine", c);
		epRT = epProvider.getEPRuntime();
		epAdmin = epProvider.getEPAdministrator();

		epl("create variant schema SysOut as *");
		epl("select * from SysOut", new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				System.out.println(newEvents[0].getUnderlying().toString());
			}
		});

		epProvider.addStatementStateListener(new EPStatementStateListener() {

			@Override
			public void onStatementStateChange(EPServiceProvider serviceProvider, EPStatement statement) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onStatementCreate(EPServiceProvider serviceProvider, EPStatement statement) {
				System.out.println("registered statement: " + statement.getText());
				for (Annotation a : statement.getAnnotations()) {
					System.out.println(a.annotationType());
					if (a.annotationType().equals(Verbose.class)) {
						System.out.println("GOOD!");
						statement.addListener(new UpdateListener() {
							@Override
							public void update(EventBean[] newEvents, EventBean[] oldEvents) {
								System.out.println(newEvents[0].getUnderlying().toString());
							}
						});
					}
				}
			}
		});

	}

	private EPServiceProvider epProvider = null;
	private EPRuntime epRT;
	private EPAdministrator epAdmin;

	public EPStatement epl(CharSequence epl, UpdateListener listener) {
		return epl(epl.toString(), listener);
	}

	public EPStatement epl(CharSequence eplStatement) {
		EPStatement result = getAdministrator().createEPL(eplStatement.toString());
		logger.info("Statement created: " + result.getName() + " as: " + eplStatement);
		return result;
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

	public EPStatement epl(String statement, UpdateListener listener) {
		EPStatement cepStatement = epAdmin.createEPL(statement);
		cepStatement.addListener(listener);
		return cepStatement;
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
