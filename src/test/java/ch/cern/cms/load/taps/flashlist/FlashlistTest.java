package ch.cern.cms.load.taps.flashlist;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.cern.cms.load.EventProcessor;
import ch.cern.cms.load.Load;
import ch.cern.cms.load.FieldTypeResolver;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

public class FlashlistTest {

	public final static long waitTime = 200;

	private static final String[] fields = { "word", "length", "vowels", "consonants" };

	public static final String REMOTE_URL = "http://akson.sgh.waw.pl/~tb47893/words";
	public static final String LOCAL_URL = "file:///home/bawey/Workspace/LOAD/dmp/words";
	public static final String STREAM_NAME = "Word";
	private static boolean callbackCalled = false;

	private Load ctl;
	private EventProcessor ep;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		/** simulate a running environment **/
		ctl = Load.getInstance();
		FieldTypeResolver ftr = Load.getInstance().getResolver();
		ftr.setFieldType("length", Long.class);
		ftr.setFieldType("vowels", Integer.class);
		ftr.setFieldType("consonants", Double.class);
		/** prepare a dummy FlashlistEventsTap **/
		AbstractFlashlistEventsTap tap = new AbstractFlashlistEventsTap(ctl, null) {
			@Override
			public void registerEventTypes(Load app) {
				Map<String, Object> def = new HashMap<String, Object>();
				for (String f : fields) {
					def.put(f, Load.getInstance().getResolver().getFieldType(f, STREAM_NAME));
				}
				ctl.getEventProcessor().getConfiguration().addEventType(STREAM_NAME, def);
			}

			@Override
			public void preRegistrationSetup(Load app) {
				// TODO Auto-generated method stub
			}

		};
		tap.preRegistrationSetup(ctl);

		tap.registerEventTypes(ctl);
		ctl.getEventProcessor().epl("select * from " + STREAM_NAME + " where vowels>1", new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				System.out.println("UpdateListener called for " + newEvents[0].getUnderlying());
				callbackCalled = true;
			}
		});

	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testParsingRemoteFile() throws MalformedURLException, InterruptedException {
		callbackCalled = false;
		URL url = new URL(REMOTE_URL);
		Flashlist fl = new Flashlist(url, STREAM_NAME);
		assertEquals(3, fl.size());
		assertEquals(Integer.class, fl.get(1).get("vowels").getClass());
		fl.emit(Load.getInstance().getEventProcessor());
		Thread.sleep(waitTime);
		Assert.assertTrue(callbackCalled);
	}

	@Test
	public void testParsingLocalFile() throws MalformedURLException, InterruptedException {
		callbackCalled = false;
		URL url = new URL(LOCAL_URL);
		Flashlist fl = new Flashlist(url, STREAM_NAME);
		assertEquals(3, fl.size());
		assertEquals(Double.class, fl.get(1).get("consonants").getClass());
		fl.emit(Load.getInstance().getEventProcessor());
		Thread.sleep(waitTime);
		Assert.assertTrue(callbackCalled);
	}

}
