package ch.cern.cms.flad;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Flad {

	public static final String CATALOG_SUFFIX = "retrieveCatalog?fmt=plain";
	public static final String LIST_SUFFIX = "retrieveCollection?fmt=plain&flash=";

	private static boolean debug = false;

	public static void setUpSOCKSProxy() {
		System.out.println("Setting up SOCKS proxy ...");

		Properties sysProperties = System.getProperties();

		// Specify proxy settings
		sysProperties.put("socksProxyHost", "127.0.0.1");
		sysProperties.put("socksProxyPort", "1080");
		sysProperties.put("proxySet", "true");
	}

	public static final void main(String[] args) {
		if (args.length < 1) {
			System.err.println("no args");
			System.exit(1);
		}

		setUpSOCKSProxy();

		if (args.length > 1 && args[1].equals("-d")) {
			debug = true;
		}
		String root = args[0];

		while (true) {
			String catalog = getHTML(root + CATALOG_SUFFIX);

			debug("catalog: ");
			debug(catalog);

			String[] lists = catalog.split("\n");

			debug("lists:");
			for (String list : lists) {
				debug(list);
			}

			Map<String, String> lastVersion = new HashMap<String, String>();
			BufferedWriter bw = null;

			for (int i = 1; i < lists.length; ++i) {
				String list = getHTML(root + LIST_SUFFIX + lists[i]);
				if (!list.equals(lastVersion.get(lists[i]))) {
					lastVersion.put(lists[i], list);
					File dir = new File(lists[i].substring(lists[i].lastIndexOf(':') + 1));
					if (!dir.exists()) {
						dir.mkdir();
					}
					if (dir.exists() && dir.isDirectory()) {
						try {
							bw = new BufferedWriter(new FileWriter(new File(dir.getName() + "/" + (new Date().getTime()))));
							bw.write(list);
							bw.close();
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					} else {
						throw new RuntimeException("What was to be a dir is a file... " + dir.getName());
					}
				}
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				System.err.println("No sleep!");
			}
		}
	}

	private static String getHTML(String urlToRead) {
		System.out.println("url: " + urlToRead);
		URL url;
		HttpURLConnection conn;
		BufferedReader rd;
		String line;
		String result = "";
		try {
			url = new URL(urlToRead);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			while ((line = rd.readLine()) != null) {
				result += (line + "\n");
			}
			rd.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(result);
		return result;
	}

	private static void debug(String s) {
		if (debug) {
			System.out.println(s);
		}
	}

}
