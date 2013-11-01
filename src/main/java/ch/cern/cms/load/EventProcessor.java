package ch.cern.cms.load;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.apache.log4j.Logger;

import ch.cern.cms.esper.Trx;
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
import com.espertech.esper.client.EPStatementState;
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

	protected EventProcessor(Load load) {
		Configuration c = new Configuration();
		c.getEngineDefaults().getExecution().setPrioritized(true);

		if (load.getSettings().containsKey(Settings.KEY_TIMER)) {
			c.getEngineDefaults().getThreading().setInternalTimerEnabled(false);
			logger.info("dropping internal timer");
		} else {
			logger.info("using internal timer");
		}

		c.getEngineDefaults().getThreading().setListenerDispatchPreserveOrder(true);
		c.addImport(HwInfo.class);
		c.addImport(CmsHw.class);
		c.addImport("ch.cern.cms.esper.annotations.*");
		c.addImport(Trx.class);

		epProvider = EPServiceProviderManager.getProvider("myCEPEngine", c);
		epAdmin = epProvider.getEPAdministrator();

		epl("create schema AbstractConclusion as (type String, title String, details String)");
		epl("create variant schema ConclusionsStream as AbstractConclusion");
		epl("create window Conclusions.win:keepall() as select * from ConclusionsStream");
		epl("insert into Conclusions select * from ConclusionsStream");

		epProvider.addStatementStateListener(new EPStatementStateListener() {

			@Override
			public void onStatementStateChange(EPServiceProvider serviceProvider, EPStatement statement) {
				if (statement.getState().equals(EPStatementState.STARTED)) {
					for (Annotation atn : statement.getAnnotations()) {
						if (atn.annotationType().equals(Conclusion.class)) {
							epl("insert into ConclusionsStream select * from " + ((Conclusion) atn).streamName());
						}
					}
				}
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
					}
				}
			}
		});

	}

	private EPServiceProvider epProvider = null;
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
		return getProvider().getEPRuntime();
	}

	public EPAdministrator getAdministrator() {
		return epAdmin;
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
		getProvider().getEPRuntime().sendEvent(event);
	}

	public static interface MultirowSubscriber {
		public void update(Map<?, ?> data);

		public void updateStart(int insertStreamLength, int removeStreamLength);

		public void updateEnd();

	}

}
