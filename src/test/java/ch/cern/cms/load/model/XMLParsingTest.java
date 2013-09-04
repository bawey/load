package ch.cern.cms.load.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONValue;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class XMLParsingTest {

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
	public void test() throws IOException, ClassNotFoundException {

		//File input = new File("/home/bawey/Desktop/problems1378127309500.xml");
		File input = new File("/stringObject");
//		BufferedReader br = new BufferedReader(new FileReader(input));
//
//		StringBuilder sb = new StringBuilder();
//		String line = null;
//
//		while ((line = br.readLine()) != null) {
//			sb.append(line);
//		}
//
//		br.close();
		
		//String meat = sb.toString();
		
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(input));
		String meat = (String) ois.readObject();
		
		meat=meat.replace("\n", "").replace("\r", "");
		System.err.println(meat);
		
		// System.out.println(sb.toString());

		Pattern p = Pattern.compile("<PARAMETER><NAME>(.*?)</NAME>|<VALUE>(.*?)</VALUE></PARAMETER>");
		Matcher matcher = p.matcher(meat);
		int i = 0;
		// while (matcher.find()) {
		// System.out.println(matcher.group(1 + (i++ % 2)));
		// }

		String key = null;
		Map<String, Object> map = new TreeMap<String, Object>();
		while (matcher.find()) {
			int mod = i++ % 2;
			String val = matcher.group(1 + mod);
			// System.out.println(val);
			if (mod == 0) {
				key = val;
				System.out.print("key: " + key);
			} else {
				System.out.println(", val: " + val);
				Object json = JSONValue.parse(val);
				map.put(key, json != null ? json : val);
			}
		}

		System.out.println("map:");
		System.out.println(map);

	}

}
