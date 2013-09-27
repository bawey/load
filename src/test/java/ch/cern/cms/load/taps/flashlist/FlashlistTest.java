package ch.cern.cms.load.taps.flashlist;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

import ch.cern.cms.load.ExpertController;
import ch.cern.cms.load.configuration.Settings;
import ch.cern.cms.load.eventProcessing.EventProcessor;
import ch.cern.cms.load.taps.flashlist.AbstractFlashlistEventsTap.FieldTypeResolver;

public class FlashlistTest {

	private static final String[] fields = { "word", "length", "vowels", "consonants" };

	public static final String REMOTE_URL = "http://localhost:10000/functionmanagers/file";
	public static final String LOCAL_URL = "file:///usr/local/apache-tomcat-6.0.29/webapps/ROOT/functionmanagers/file";
	public static final String STREAM_NAME = "Word";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		/** simulate a running environment **/
		ExpertController ctl = ExpertController.getInstance();
		/** prepare a dummy FlashlistEventsTap **/
		AbstractFlashlistEventsTap tap = new AbstractFlashlistEventsTap() {
			@Override
			public void registerEventTypes(EventProcessor eps) {
				Map<String, Object> def = new HashMap<String, Object>();
				for (String f : fields) {
					def.put(f, ((FieldTypeResolver) Settings.getInstance().get(AbstractFlashlistEventsTap.PKEY_FIELD_TYPE)).getFieldType(f,
							STREAM_NAME));
				}
				EventProcessor.getInstance().getConfiguration().addEventType(STREAM_NAME, def);
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
		tap.registerEventTypes(EventProcessor.getInstance());
		// empty anyway
		tap.openStreams(EventProcessor.getInstance());

		EventProcessor.getInstance().registerStatement("select * from " + STREAM_NAME + " where vowels>1", new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				System.out.println("hey");
			}
		});

	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testParsingRemoteFile() throws MalformedURLException {
		URL url = new URL(REMOTE_URL);
		Flashlist fl = new Flashlist(url, STREAM_NAME);
		assertEquals(2, fl.size());
		assertEquals(Integer.class, fl.get(1).get("vowels").getClass());
		fl.emit(EventProcessor.getInstance());
	}

	@Test
	public void testParsingLocalFile() throws MalformedURLException {
		URL url = new URL(LOCAL_URL);
		Flashlist fl = new Flashlist(url, STREAM_NAME);
		assertEquals(2, fl.size());
		assertEquals(Double.class, fl.get(1).get("consonants").getClass());
		fl.emit(EventProcessor.getInstance());
	}

}
