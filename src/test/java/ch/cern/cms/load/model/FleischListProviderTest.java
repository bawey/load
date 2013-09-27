package ch.cern.cms.load.model;

import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

import ch.cern.cms.load.eventProcessing.EventProcessor;

public class FleischListProviderTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		FlashListDatatypeDictionary fldd = FlashListDatatypeDictionary.getInstance();
		fldd.setFieldType("outputIntegralTimeOOSB", Integer.class);
		fldd.setFieldType("outputFractionWarningA", Double.class);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws IOException {
		EventProcessor ep = EventProcessor.getInstance();
		FleischListProvider flp = new FleischListProvider("/home/bawey/Desktop/flashlists/41/");
		flp.registerEventTypes(ep);

		// ep.registerStatement("select * from FMMStatus where outputFractionWarningA > 0.1",
		// printer);
		//ep.registerStatement("select DAQ_STATE, TRG_STATE, TRACKER_STATE from levelZeroFM_dynamic", printer);
		
		ep.registerStatement("select * from pattern[every levelZeroFM_dynamic(DAQ_STATE='Running',TRG_STATE='Running') -> levelZeroFM_dynamic(DAQ_STATE!='Running', TRG_STATE!='Running')]", new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				System.out.println("Both DAQ and TRG went from running to sth else");
			}
		});
		
		ep.registerStatement("select * from pattern[every levelZeroFM_dynamic(DAQ_STATE='Running',TRG_STATE='Running') -> levelZeroFM_dynamic(DAQ_STATE!='Running', TRG_STATE='Running')]", new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				System.out.println("Just DAQ went to sth else than running");
			}
		});
		
		ep.registerStatement("select * from pattern[every levelZeroFM_dynamic(DAQ_STATE='Running',TRG_STATE='Running') -> levelZeroFM_dynamic(DAQ_STATE='Running', TRG_STATE!='Running')]", new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				System.out.println("Just TRG went from running to sth else");
			}
		});
		
		
		flp.emit(ep, 1000000);

		
		// System.out.println("registered events: " +
		// ep.getConfiguration().getEventTypes());
	}

	private static UpdateListener printer = new UpdateListener() {
		@Override
		public void update(EventBean[] newEvents, EventBean[] oldEvents) {
			System.out.println("printer: " + newEvents[0].getUnderlying().toString());
		}
	};
	

	
	
}
