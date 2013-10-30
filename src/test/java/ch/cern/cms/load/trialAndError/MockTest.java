package ch.cern.cms.load.trialAndError;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

import ch.cern.cms.load.EventProcessor;
import ch.cern.cms.load.Load;
import ch.cern.cms.load.eventData.Mock;

/**
 *	Changing states vs. broadcasting each active alert with an incoming event 
 *
 */

public class MockTest {

	public static final String MOCK = Mock.class.getName();

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		EventProcessor ep = Load.getInstance().getEventProcessor();
		ep.getConfiguration().addEventType(Mock.class);
		ep.epl("select * from " + MOCK, new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				System.out.println(newEvents[0].getUnderlying());
			}
		});

		//ep.createEPL("create window Stats.std:unique(cores) as (cores int, throughput double)");
		
		/** one way of doing it, so-so **/
		ep.epl("select status, count(*) as qty from " + MOCK
				+ ".win:time(5 sec) group by status having status!='error' and count(*)=0", fiveSecError);
		/** as in patterns **/
		ep.epl("select * from pattern [ every (timer:interval(5 sec) and not "+MOCK+"(fractionBusy <= 0.001)) ]", busyFraction);
		/** trigger jump **/
		ep.epl("select * from pattern [every a="+MOCK+" -> b="+MOCK+"(rate>a.rate*1.15 or rate<a.rate*0.85)]", triggerJump);

		Mock.playEvents(ep, 1000);

	}

	private UpdateListener fiveSecError = new UpdateListener() {
		@Override
		public void update(EventBean[] newEvents, EventBean[] oldEvents) {
			System.out.println(">> 5 seconds in error state!");
		}
	};

	private UpdateListener busyFraction = new UpdateListener() {
		@Override
		public void update(EventBean[] newEvents, EventBean[] oldEvents) {
			System.out.println(">> fractionBusy alert!");
		}
	};

	private UpdateListener triggerJump = new UpdateListener() {
		@Override
		public void update(EventBean[] newEvents, EventBean[] oldEvents) {
			System.out.println(">> trigger jump!");
		}
	};

}
