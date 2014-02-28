package ch.cern.cms.load;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.apache.log4j.Logger;

import ch.cern.cms.esper.CustomConcatFactory;
import ch.cern.cms.esper.Trx;
import ch.cern.cms.esper.annotations.Conclusion;
import ch.cern.cms.esper.annotations.Verbose;
import ch.cern.cms.esper.annotations.Watched;
import ch.cern.cms.load.eventData.FedMask;
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
import com.espertech.esper.client.time.CurrentTimeEvent;

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
		Load load = Load.getInstance();

		Configuration c = new Configuration();
		c.getEngineDefaults().getExecution().setPrioritized(true);

		if (!load.isInternalTimerEnabled()) {
			c.getEngineDefaults().getThreading().setInternalTimerEnabled(false);
			logger.info("dropping internal timer");
		} else {
			logger.info("using internal timer");
		}

		c.getEngineDefaults().getThreading().setListenerDispatchPreserveOrder(true);
		c.addImport(HwInfo.class);
		c.addImport(CmsHw.class);
		c.addImport(Math.class);
		c.addImport("ch.cern.cms.esper.annotations.*");
		c.addImport(Trx.class);
		c.addPlugInSingleRowFunction("caseless_in", Trx.class.getCanonicalName(), "inIgnoreCase");
		c.addPlugInSingleRowFunction("date", Trx.class.getCanonicalName(), "toDate");
		c.addPlugInSingleRowFunction("time_span", Trx.class.getCanonicalName(), "timeSpan");
		c.addPlugInSingleRowFunction("Integer", Trx.class.getCanonicalName(), "toInteger");
		c.addPlugInSingleRowFunction("int", Trx.class.getCanonicalName(), "toInt");
		c.addPlugInSingleRowFunction("text", Trx.class.getCanonicalName(), "toText");
		c.addPlugInSingleRowFunction("Long", Trx.class.getCanonicalName(), "toLong");
		c.addPlugInSingleRowFunction("Double", Trx.class.getCanonicalName(), "toDouble");
		c.addPlugInSingleRowFunction("arraysToMap", Trx.class.getCanonicalName(), "arraysToMap");
		c.addPlugInSingleRowFunction("arraysToSingletonMaps", Trx.class.getCanonicalName(), "arraysToSingletonMaps");

		c.addPlugInSingleRowFunction("fmm", HwInfo.class.getCanonicalName(), "getFMM");
		c.addPlugInSingleRowFunction("fedSrcId", HwInfo.class.getCanonicalName(), "getFedSrcId");

		c.addPlugInSingleRowFunction("mainFedSrcIds", HwInfo.class.getCanonicalName(), "getMainFedSrcIds");
		c.addPlugInSingleRowFunction("fedsInfoString", HwInfo.class.getCanonicalName(), "fedsInfoString");
		c.addPlugInSingleRowFunction("getPartitionName", HwInfo.class.getCanonicalName(), "getPartitionName");

		c.addPlugInSingleRowFunction("parseFem", FedMask.class.getCanonicalName(), "parse");
		c.addPlugInSingleRowFunction("activeFedSrcIds", FedMask.class.getCanonicalName(), "getActiveFedSrcIds");
		c.addPlugInSingleRowFunction("in_array", Trx.class.getCanonicalName(), "inArray");
		c.addPlugInSingleRowFunction("reformat", Trx.class.getCanonicalName(), "reformat");
		c.addPlugInSingleRowFunction("tuple", Trx.class.getCanonicalName(), "tuple");
		c.addPlugInSingleRowFunction("is_nonvariant", Trx.class.getCanonicalName(), "isNonvariant");
		c.addPlugInSingleRowFunction("formatMs", Trx.class.getCanonicalName(), "formatMs");
		c.addPlugInSingleRowFunction("format", Trx.class.getCanonicalName(), "format");
		c.addPlugInSingleRowFunction("regExtract", Trx.class.getCanonicalName(), "regExtract");
		c.addPlugInSingleRowFunction("getBusyProcessorsRatio", Trx.class.getCanonicalName(), "getBusyProcessorsRatio");
		c.addPlugInSingleRowFunction("indexesOf", Trx.class.getCanonicalName(), "indexesOf");
		c.addPlugInSingleRowFunction("subList", Trx.class.getCanonicalName(), "subList");
		c.addPlugInSingleRowFunction("compareHostnames", Trx.class.getCanonicalName(), "compareHostnames");
		c.addPlugInSingleRowFunction("fedsHistogram", HwInfo.class.getCanonicalName(), "fedsHistogram");
		
		
		// c.addPlugInAggregationFunctionFactory("concat", CustomConcatFunction.class.getCanonicalName());
		c.addPlugInAggregationFunctionFactory("concat", CustomConcatFactory.class.getCanonicalName());

		// this might be a nice way to define the timestamps relationship
		// c.addPlugInPatternGuard(namespace, name, guardFactoryClass)

		// c.addPlugInSingleRowFunction("", HwInfo.class.getCanonicalName(),
		// "");

		epProvider = EPServiceProviderManager.getProvider("myCEPEngine", c);
		if (load.getSettings().containsKey(Settings.KEY_TIMER_START)) {
			epProvider.getEPRuntime().sendEvent(new CurrentTimeEvent(Long.parseLong(load.getSettings().getProperty(Settings.KEY_TIMER_START)) - 1));
		}
		epAdmin = epProvider.getEPAdministrator();

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
						for (EventSink view : Load.getInstance().getViews()) {
							statement.addListener(view.getVerboseStatementListener());
						}
					} else if (a.annotationType().equals(Watched.class)) {
						for (EventSink view : Load.getInstance().getViews()) {
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
