package ch.cern.cms.load.taps.flashlist;

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

import org.apache.log4j.Logger;

import ch.cern.cms.load.ExpertController;

/**
 * urn:xdaq-flashlist:FMMInput
 * 
 * urn:xdaq-flashlist:FMMInputDetail
 * 
 * urn:xdaq-flashlist:FMMStatus
 * 
 * urn:xdaq-flashlist:frlBxHisto
 * 
 * urn:xdaq-flashlist:frlWcHisto
 * 
 * urn:xdaq-flashlist:frlcontrollerCard
 * 
 * urn:xdaq-flashlist:frlcontrollerLink
 * 
 * urn:xdaq-flashlist:frlcontrollerStatus
 * 
 * urn:xdaq-flashlist:hostInfo
 * 
 * urn:xdaq-flashlist:levelZeroFM_dynamic
 * 
 * urn:xdaq-flashlist:levelZeroFM_static
 * 
 * urn:xdaq-flashlist:levelZeroFM_subsys
 */

/**
 * in case of need of the link: http://srv-c2d04-19.cms:9941/urn:xdaq-application:lid=400/
 * 
 * @author Tomasz Bawej
 * 
 */

public class OnlineFlashlistEventsTap extends AbstractFlashlistEventsTap {

	public static final String FLASHLISTS_ROOT = "http://srv-c2d04-19.cms:9941/urn:xdaq-application:lid=400/";
	public static final String CATALOG_SUFFIX = "retrieveCatalog?fmt=plain";
	public static final String LIST_SUFFIX = "retrieveCollection?fmt=plain&flash=";

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

	Logger logger = Logger.getLogger(OnlineFlashlistEventsTap.class);

	private OnlineFlashlistEventsTap(ExpertController expert, String path) {
		super(expert, path);
		job = new Runnable() {
			@Override
			public void run() {
				while (true) {
					String catalog = getHTML(FLASHLISTS_ROOT + CATALOG_SUFFIX);
					String[] lists = catalog.split("\n");

					Map<String, String> lastVersion = new HashMap<String, String>();
					BufferedWriter bw = null;

					for (int i = 1; i < lists.length; ++i) {
						String list = getHTML(FLASHLISTS_ROOT + LIST_SUFFIX + lists[i]);
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
		};
	}

	/** need to connect to server, bla bla bla **/
	@Override
	public void preRegistrationSetup() {

	}

	@Override
	public void registerEventTypes() {
		// TODO Auto-generated method stub
	}

}
