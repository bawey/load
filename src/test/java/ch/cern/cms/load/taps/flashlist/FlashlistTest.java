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
import ch.cern.cms.load.ExpertController;
import ch.cern.cms.load.Settings;
import ch.cern.cms.load.taps.flashlist.AbstractFlashlistEventsTap.FieldTypeResolver;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

public class FlashlistTest {

	public final static long waitTime = 200;

	private static final String[] fields = { "word", "length", "vowels", "consonants" };

	public static final String REMOTE_URL = "http://akson.sgh.waw.pl/~tb47893/words";
	public static final String LOCAL_URL = "file:///home/bawey/Workspace/load/dmp/words";
	public static final String STREAM_NAME = "Word";
	private static boolean callbackCalled = false;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		/** simulate a running environment **/
		final ExpertController ctl = ExpertController.getInstance();
		/** prepare a dummy FlashlistEventsTap **/
		AbstractFlashlistEventsTap tap = new AbstractFlashlistEventsTap() {
			@Override
			public void registerEventTypes(EventProcessor eps) {
				Map<String, Object> def = new HashMap<String, Object>();
				for (String f : fields) {
					def.put(f, ((FieldTypeResolver) ctl.getSettings().get(AbstractFlashlistEventsTap.PKEY_FIELD_TYPE))
							.getFieldType(f, STREAM_NAME));
				}
				ctl.getEventProcessor().getConfiguration().addEventType(STREAM_NAME, def);
			}

			@Override
			public void openStreams(EventProcessor eps) {
				// TODO Auto-generated method stub
			}
		};
		tap.defineProperties(ctl);

		{
			FieldTypeResolver ftr = (FieldTypeResolver) ctl.getSettings().get(AbstractFlashlistEventsTap.PKEY_FIELD_TYPE);
			ftr.setFieldType("length", Long.class);
			ftr.setFieldType("vowels", Integer.class);
			ftr.setFieldType("consonants", Double.class);
		}
		tap.registerEventTypes(ctl.getEventProcessor());
		// empty anyway
		tap.openStreams(ctl.getEventProcessor());

		ctl.getEventProcessor().registerStatement("select * from " + STREAM_NAME + " where vowels>1", new UpdateListener() {
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
		fl.emit(ExpertController.getInstance().getEventProcessor());
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
		fl.emit(ExpertController.getInstance().getEventProcessor());
		Thread.sleep(waitTime);
		Assert.assertTrue(callbackCalled);
	}

}
