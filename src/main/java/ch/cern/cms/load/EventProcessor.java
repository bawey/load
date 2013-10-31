package ch.cern.cms.load;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.apache.log4j.Logger;

import ch.cern.cms.esper.annotations.Conclusion;
import ch.cern.cms.esper.annotations.Verbose;
import ch.cern.cms.esper.annotations.Watched;
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
		c.getEngineDefaults().getExecution().setPrioritized(true);
		// c.getEngineDefaults().getThreading().setInternalTimerEnabled(false);
		c.getEngineDefaults().getThreading().setListenerDispatchPreserveOrder(true);
		c.addImport(HwInfo.class);
		c.addImport(CmsHw.class);
		c.addImport("ch.cern.cms.esper.annotations.*");

		epProvider = EPServiceProviderManager.getProvider("myCEPEngine", c);
		epRT = epProvider.getEPRuntime();
		epAdmin = epProvider.getEPAdministrator();

		epl("create schema AbstractConclusion as (type String, title String, details String)");
		epl("create variant schema ConclusionsStream as AbstractConclusion");
		epl("create window Conclusions.win:keepall() as select * from ConclusionsStream");
		epl("insert into Conclusions select * from ConclusionsStream");

		// proof of concept
		// epl("create schema TestConclusion(number Double) inherits AbstractConclusion");
		//
		// epl("create variable int nr = 666");
		// epl("on pattern[every timer:interval(1 msec)] set nr = nr+2");
		//
		// epl("on pattern[every timer:interval(1 msec)] "
		// +
		// "insert into TestConclusion select 'error' as type, 'sadas' as title, 'ddd' as details, nr as number");
		//
		// epl("insert into ConclusionsStream select * from TestConclusion");
		//
		// epl("select * from Conclusions", new UpdateListener() {
		// @Override
		// public void update(EventBean[] newEvents, EventBean[] oldEvents) {
		// System.out.println(newEvents[0].getUnderlying());
		// }
		// });

		epProvider.addStatementStateListener(new EPStatementStateListener() {

			@Override
			public void onStatementStateChange(EPServiceProvider serviceProvider, EPStatement statement) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onStatementCreate(EPServiceProvider serviceProvider, EPStatement statement) {
				for (Annotation a : statement.getAnnotations()) {
					if (a.annotationType().equals(Verbose.class)) {
						for (LoadView view : Load.getInstance().getViews()) {
							statement.addListener(view.getVerboseStatementListener());
						}
					} else if (a.annotationType().equals(Watched.class)) {
						for (LoadView view : Load.getInstance().getViews()) {
							statement.addListener(view.getWatchedStatementListener());
						}
					} else if (a.annotationType().equals(Conclusion.class)) {
						epl("insert into ConclusionsStream select * from " + ((Conclusion) a).streamName());
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
