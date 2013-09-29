package ch.cern.cms.load.taps.flashlist;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ch.cern.cms.load.configuration.Settings;
import ch.cern.cms.load.eventProcessing.EventProcessor;
import ch.cern.cms.load.taps.flashlist.AbstractFlashlistEventsTap.FieldTypeResolver;

public class Flashlist extends LinkedList<Map<String, Object>> {

	private static final long serialVersionUID = 1L;
	private String streamName;
	private Settings settings = Settings.getInstance();
	private static FieldTypeResolver resolver = null;
	
	
	public Flashlist(URL url, String listName) {
		super();
		if (resolver == null) {
			linkFieldDefinitions();
		}
		this.streamName = listName;
		String[] keys = null;
		try {
			URLConnection conn = url.openConnection();
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line = null;
			while ((line = br.readLine()) != null) {
				if (keys == null) {
					keys = line.split(",");
				} else {
					Map<String, Object> map = new HashMap<String, Object>();
					String[] tokens = smartSplit(line);
					if (tokens.length != keys.length) {
						throw new RuntimeException("tokens and keys lengths differ for: " + url);
					}
					for (int i = 0; i < tokens.length; ++i) {
						map.put(keys[i], resolver == null ? tokens[i] : resolver.convert(tokens[i], keys[i], listName));
					}
					this.add(map);
				}
			}
			br.close();
		} catch (IOException e) {
			throw new RuntimeException("No mistakes allowed!", e);
		}
	}

	public static List<Map<String, String>> parse(File file) throws IOException {
		List<Map<String, String>> results = null;
		return results;
	}

	public void emit(EventProcessor ep) {
		for (Map<String, Object> event : this) {
			System.out.println("sending " + streamName + ": " + event);
			ep.getRuntime().sendEvent(event, streamName);
		}
	}

	public static String[] smartSplit(String str) {
		List<String> lst = new LinkedList<String>();
		int a = 0, b = 0;

		while (a < str.length() && b < str.length()) {
			// a has to stop on opening quote, b on closing
			while (str.charAt(a) != '"') {
				++a;
			}
			while (str.charAt(b) != '"' || b == a) {
				if (isCloseable(str.charAt(b))) {
					b = findClosing(str, b);
				} else {
					++b;
				}
			}
			lst.add(str.substring(a + 1, b));
			a = ++b;
		}
		return lst.toArray(new String[lst.size()]);
	}

	private static boolean isCloseable(char c) {
		return (c == '[' || c == '{');
	}

	private static char closingChar(char c) {
		switch (c) {
		case '[':
			return ']';
		case '{':
			return '}';
		default:
			return 0;
		}
	}

	private static int findClosing(String s, int pos) {
		char c = s.charAt(pos++);
		while (s.charAt(pos) != closingChar(c)) {
			if (isCloseable(s.charAt(pos))) {
				pos = findClosing(s, pos);
			} else {
				++pos;
			}
		}
		return pos;
	}

	private static synchronized void linkFieldDefinitions() {
		if (resolver == null) {
			if (resolver == null) {
				Object value = Settings.getInstance().get(AbstractFlashlistEventsTap.PKEY_FIELD_TYPE);
				if (value instanceof FieldTypeResolver) {
					resolver = (FieldTypeResolver) value;
				} else {
					throw new RuntimeException("Expected to find an object of " + FieldTypeResolver.class.getSimpleName());
				}
			}
		}
	}
}
