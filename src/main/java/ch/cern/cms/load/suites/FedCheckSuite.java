package ch.cern.cms.load.suites;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

import ch.cern.cms.load.EventProcessor;
import ch.cern.cms.load.Stream;
import ch.cern.cms.load.taps.flashlist.AdHocTapTest;

public class FedCheckSuite extends AbstractCheckSuite {

	public FedCheckSuite(EventProcessor ep) {
		super(ep);
	}

	public static final String VAR_SESSION_ID = "SESSION_ID";
	public static final String VAR_AVG_TRIGGER_RATE = "AVG_TRIGGER_RATE";

	private void ep(EventProcessor ep) {
		/**
		 * create for EVM data. Keep only the most recent record per context-lid pair. Also, discard events older than 30 seconds
		 **/
		ep.createEPL("create window subrates.std:unique(url,lid).win:time(" + 30000 / pace + " msec) as (subrate double, url String, lid String)");

		ep.createEPL("insert into subrates select deltaN/deltaT as subrate, context as url, lid from EVM where sessionid=sid and deltaT>0");
		/**
		 * keep the last relevant subrate timestamp in a variable TODO: might arrive out-of-order!
		 **/
		ep.createEPL("create variable String lastEVMtimestamp = ''");
		ep.createEPL("on EVM(sessionid=sid) as evm set lastEVMtimestamp=evm.timestamp");
		ep.registerStatement("select lastEVMtimestamp as value, 'lastEVM time' as label from EVM", watchUpdater);

		/** create a window to contain observed rates **/
		ep.createEPL("create window rates.win:time(" + 60000 / pace + " sec) as (rate double)");
		/** and fill it periodically **/
		ep.createEPL("on pattern[every timer:interval(" + (1000 / pace) + " msec)] insert into rates select sum(subrate) as rate from subrates");
		ep.registerStatement("select prior(1,rate) as value, 'trigger rate' as label, lastEVMtimestamp as timestamp from rates", watchUpdater);

		/** avg rate needs to be updated only when there is sth to average **/
		ep.createEPL("create window RatesLength.win:length(1) as (length long)");
		ep.createEPL("on rates insert into RatesLength(length) select count(*) from rates");
		/**
		 * create variable for rate reference level (avg rate observed over last minute)
		 **/
		ep.createEPL("create variable double avgRate=0");
		ep.createEPL("on RatesLength(length>0) set avgRate = (select Math.round(avg(rate)) from rates)");

		ep.registerStatement("select avgRate as value, lastEVMtimestamp as timestamp, 'avg rate' as label from rates", watchUpdater);

		ep.registerStatement("select avgRate, a.rate, lastEVMtimestamp as timestamp from pattern[every a=rates(rate>(avgRate*1.3) or rate < (avgRate*0.7))]",
				new UpdateListener() {
					@Override
					public void update(EventBean[] newEvents, EventBean[] oldEvents) {
						AdHocTapTest.this.console("rate jumps", newEvents[0].getUnderlying().toString());
						ep.getRuntime().executeQuery("delete from rates");
					}
				});

		ep.registerStatement("select count(*) as value, 'totalEVM' as label from EVM", watchUpdater);
	}

	@Override
	public void registerViews() {

	}

	@Override
	public void registerVariables() {
		epl("create variable String " + VAR_SESSION_ID + "=''");
	}

	@Override
	public void registerLogic() {
		epl("on " + Stream.levelZeroFM_dynamic + "(SID!=sid, FMURL like '%PublicGlobal%') as l set sid=l.SID");
	}

}
