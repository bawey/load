package ch.cern.cms.load.taps.flashlist;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ch.cern.cms.load.Load;
import ch.cern.cms.load.utils.HttpTools;

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

	public static final String SETTINGS_KEY_FLASHLIST_ROOT = "onlineFlashlistsRoot";
	
	public static final String CATALOG_SUFFIX = "retrieveCatalog?fmt=plain";
	public static final String LIST_SUFFIX = "retrieveCollection?fmt=plain&flash=";

	private static Logger logger = Logger.getLogger(OnlineFlashlistEventsTap.class);

	private Map<String, Map<String, Object>> eventDefinitions;
	private List<String> flashlists;

	public OnlineFlashlistEventsTap(Load expert, String path) {
		super(expert, path);
		job = new Runnable() {
			@Override
			public void run() {
				while (new Date().getTime() > 0) {
					for (String flashlist : flashlists) {
						URL flashlistUrl;
						String flashlistPath = OnlineFlashlistEventsTap.this.path + LIST_SUFFIX + flashlist;
						try {
							flashlistUrl = new URL(flashlistPath);
							logger.info("creating flashlist from: " + flashlistUrl.toString());
							Flashlist fl = new Flashlist(flashlistUrl, extractFlashlistEventName(flashlist));
							fl.emit(OnlineFlashlistEventsTap.this.ep);
						} catch (MalformedURLException e) {
							logger.error("Failed to retrieve a flashlist for: " + flashlistPath);
						}
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
	}

	/** gather the information about event types **/
	@Override
	public void preRegistrationSetup(Load expert) {
		eventDefinitions = new HashMap<String, Map<String, Object>>();
		String catalog = HttpTools.getHTML(path + CATALOG_SUFFIX);
		logger.info("catalog: \n" + catalog);
		this.flashlists = new LinkedList<String>();
		for (String tkn : catalog.split("\n")) {
			if (tkn.contains(":")) {
				logger.info("adding token: " + tkn);
				flashlists.add(tkn);
			}
		}
		BufferedReader br = null;
		for (int i = 0; i < flashlists.size(); ++i) {
			String flashlistContent = HttpTools.getHTML(path + LIST_SUFFIX + flashlists.get(i));
			String eventName = extractFlashlistEventName(flashlists.get(i));
			Map<String, Object> types = new HashMap<String, Object>();

			br = new BufferedReader(new StringReader(flashlistContent));
			try {
				for (String field : br.readLine().split(",")) {
					types.put(field, Load.getInstance().getResolver().getFieldType(field, flashlists.get(i)));
					logger.info("event: " + eventName + ", field: " + field);
				}
				types.put("fetchstamp", Long.class);
				eventDefinitions.put(eventName, types);
			} catch (IOException e) {
				logger.error("Error reading from String, event definition: " + eventName + " skipped", e);
			}
		}
	}

	@Override
	public void registerEventTypes(Load expert) {
		for (String eventName : eventDefinitions.keySet()) {
			logger.info("registering event type: " + eventName);
			ep.getAdministrator().getConfiguration().addEventType(eventName, eventDefinitions.get(eventName));
		}
	}
}
