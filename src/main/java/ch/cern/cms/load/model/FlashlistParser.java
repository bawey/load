package ch.cern.cms.load.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FlashlistParser {
	public static Collection<Object> createPojos(File input) {
		List<Object> results = new LinkedList<Object>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(input));
			StringBuilder content = new StringBuilder();
			String line = null;

			Map<String, Object> root = new HashMap<String, Object>();
			Map parent = root;

			while ((line = reader.readLine()) != null) {
				content.append(line);

				if (line.contains("=>")) {
					String[] tokens = line.split("=>");
					String key = tokens[0].trim();
					String val = tokens[1].trim();
					if (val.contains("{") || val.contains("[")) {
						System.out.println("nested sth, aborthing");
						System.out.println(root);
						System.exit(19);
					} else {
						parent.put(key, val);
					}
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return results;
	}

	private Object explode(String chunk) {

		Map<String, String> map = new HashMap<String, String>();
		chunk = chunk.substring(chunk.indexOf("{") + 1, chunk.lastIndexOf("}")).trim();

		System.out.println(chunk);
		return map;
	}

	/** finds nested objects **/
	private static void findNests(String str) {
		Map<String, String> map = new HashMap<String, String>();
		int b = 0, a = 0;
		String key = null;
		int opened = 0;
		char c = 0;
		for (int i = 0; i < str.length(); c = str.charAt(i++)) {
			if (c == '{' && (++opened) == 1) {
				a = i;
				key = str.substring(b, a);
				System.out.println(key);
			} else if (c == '}' && (--opened) == 0) {
				b = i;
				map.put(key, str.substring(a + 1, b).trim());
			}
		}
		System.out.println(map);
	}
}
