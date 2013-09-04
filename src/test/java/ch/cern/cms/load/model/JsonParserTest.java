package ch.cern.cms.load.model;

/*
 *	This was to make sure about data types returned from JSON parser. These are
 *	Long for integers
 *	Double for floating point numbers  
 *	String for strings
 *	Boolean for booleans
 *	Collections and Maps as well.
 */
import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class JsonParserTest {

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
		Object integer = JSONValue.parse("3");
		Object fraction = JSONValue.parse("3.14");
		Object bool = JSONValue.parse("false");

		assertEquals(Long.class, integer.getClass());
		assertEquals(Double.class, fraction.getClass());
		assertEquals(Boolean.class, bool.getClass());

		String jsonObject = "{\"organization\": \"cern\", \"based\": \"switzerland\", \"departments\": [\"PH\",\"Dummy\"], \"isBad\": false, \"founded\": 1954, \"stuff\":{\"A-Team\": \"Boss A\", \"number\":3.1415}}";
		JSONObject cern = (JSONObject) JSONValue.parse(jsonObject);

		for (Object key : cern.keySet()) {
			introduce(cern.get(key));
		}
	}

	private void introduce(Object o) {
		System.out.println(o + " of class " + o.getClass().getSimpleName());
		if (o instanceof Collection<?>) {
			for (Object o2 : (Collection<?>) o) {
				introduce(o2);
			}
		} else if (o instanceof Map<?, ?>) {
			Map<?, ?> m = (Map<?, ?>) o;
			for (Object k : m.keySet()) {
				introduce(m.get(k));
			}
		}

	}
}
