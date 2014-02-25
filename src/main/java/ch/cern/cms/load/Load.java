package ch.cern.cms.load;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import ch.cern.cms.load.eplProviders.BasicStructEplProvider;
import ch.cern.cms.load.eplProviders.FileBasedEplProvider;
import ch.cern.cms.load.taps.flashlist.DataBaseFlashlistEventsTap;
import ch.cern.cms.load.taps.flashlist.OfflineFlashlistEventsTap;

/**
 * Core components, singletons etc. should be initialized here. However, the setup of initial structure should be well separated from the
 * configuration-specific actions to enable unit-testing and reconfigurations.
 */

// try this for loading properties: inputFile =
// this.getClass().getClassLoader().getResourceAsStream("etc/trivia.epl")

public class Load {

	private static final Logger logger = Logger.getLogger(Load.class);
	private static Load instance;

	public static Load getInstance() {
		if (instance == null) {
			synchronized (Load.class) {
				if (instance == null) {
					System.out.println("getting new LOAD instance for " + Thread.currentThread().getName());
					instance = new Load();

					instance.ep = new EventProcessor();
					instance.resolver = new FieldTypeResolver();
					instance.settings.put(Settings.KEY_RESOLVER, instance.resolver);
				}
			}
		}
		return instance;
	}

	/**
	 * @param args
	 */
	public static final void main(String[] args) {
		Thread.currentThread().setName("Level 0 Anomaly Detective");
		instance = getInstance();
		if (instance.settings.getProperty(DataBaseFlashlistEventsTap.KEY_DB_MODE, "read").equalsIgnoreCase("write")) {
			MysqlDumper.main(args);
		} else {
			instance.defaultSetup();
		}
	}

	private final Settings settings;

	private EventProcessor ep;

	private final Set<EventsTap> taps = new HashSet<EventsTap>();

	private FieldTypeResolver resolver;

	private final Set<EventSink> views = new HashSet<EventSink>();

	private Load() {
		// settings have to go first!
		System.out.println("producing LOAD");
		settings = new Settings();
		setUpSOCKSProxy();
	}

	public EventProcessor getEventProcessor() {
		return ep;
	}

	public Settings getSettings() {
		return settings;
	}

	public FieldTypeResolver getResolver() {
		return resolver;
	}

	public Set<EventSink> getViews() {
		return views;
	}

	/**
	 * Initializes the default application structure.
	 */
	private void defaultSetup() {
		// looks stupid - calls every known component to perform its setup
		// depending on the config file's contents
		// place for dependency injection??

		// register the views
		this.setUpViews();

		// register taps
		if (settings.getMany(OfflineFlashlistEventsTap.SETTINGS_KEY_FLASHLIST_DIR).size() > 0) {
			// registerTap(new OfflineFlashlistEventsTap(this, null));
			// registerTap(new OfflineFlashlistEventsTap2(this));
		}
		if (settings.getProperty(DataBaseFlashlistEventsTap.KEY_DB_MODE).equalsIgnoreCase("read")) {
			registerTap(new DataBaseFlashlistEventsTap(this));
		}
		EventsTap.registerKnownOnlineTaps();
		// register the statements from file
		new BasicStructEplProvider().registerStatements(ep);
		new FileBasedEplProvider().registerStatements(ep);

		openTaps();
	}

	public void openTaps() {
		for (EventsTap et : taps) {
			et.openStreams();
		}
	}

	/**
	 * Inserts into the list of known taps this should depend on some configuration later on or start-up choice
	 */
	public void registerTap(EventsTap tap) {
		taps.add(tap);
	}

	public Collection<EventsTap> getTaps() {
		return this.taps;
	}

	private void setUpSOCKSProxy() {
		for (String key : new String[] { "socksProxyHost", "proxySet", "socksProxyPort" }) {
			System.getProperties().put(key, settings.get(key));
		}
	}

	private void setUpViews() {
		for (String viewName : settings.getMany("view")) {
			Object sink = instantiateComponent("sink", viewName);
			if (sink instanceof EventSink) {
				views.add((EventSink) sink);
			}
			if (sink instanceof LogSink) {
				LoadLogCollector.registerSink((LogSink) sink);
			}
		}
	}

	private Object instantiateComponent(String type, String id) {
		ClassLoader ldr = this.getClass().getClassLoader();
		try {
			String className = this.getClass().getPackage().getName() + "." + type + "s" + "." + id;
			logger.info("loading class: " + className);
			return ldr.loadClass(className).newInstance();

		} catch (Exception e) {
			logger.error("Failed to load " + type + " component: " + id, e);
			e.printStackTrace();
			return null;
		}
	}

	public boolean isInternalTimerEnabled() {
		return false;
	}

}
