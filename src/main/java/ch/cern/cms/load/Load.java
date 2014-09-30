package ch.cern.cms.load;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import ch.cern.cms.load.eplProviders.BasicStructEplProvider;
import ch.cern.cms.load.eplProviders.FileBasedEplProvider;
import ch.cern.cms.load.taps.flashlist.DataBaseFlashlistEventsTap;
import ch.cern.cms.load.taps.flashlist.OnlineFlashlistEventsTap;

/**
 * Core components, singletons etc. should be initialized here. However, the
 * setup of initial structure should be well separated from the
 * configuration-specific actions to enable unit-testing and reconfigurations.
 */

public class Load {

	private static Load instance;
	private static final Logger logger = Logger.getLogger(Load.class);

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

	public static final void main(String[] args) {
		Thread.currentThread().setName("Level 0 Anomaly Detective");
		instance = getInstance();
		if (instance.settings.getProperty(DataBaseFlashlistEventsTap.KEY_DB_MODE, "read").equalsIgnoreCase("write")) {
			MysqlDumper.main(args);
		} else {
			instance.defaultSetup();
		}
	}

	private EventProcessor ep;

	private FieldTypeResolver resolver;

	private final Settings settings;

	private final Set<EventsTap> taps = new HashSet<EventsTap>();

	private final Set<EventsSink> views = new HashSet<EventsSink>();

	private Load() {
		settings = new Settings();
		setUpSOCKSProxy();
	}

	public EventProcessor getEventProcessor() {
		return ep;
	}

	public FieldTypeResolver getResolver() {
		return resolver;
	}

	public Settings getSettings() {
		return settings;
	}

	public Set<EventsSink> getViews() {
		return views;
	}

	/**
	 * Initializes the default application structure.
	 */
	private void defaultSetup() {
		this.setUpViews();

		if (settings.check(DataBaseFlashlistEventsTap.KEY_DB_MODE, "read")) {
			registerTap(new DataBaseFlashlistEventsTap(this));
		}

		for (String flashlistsRootUrl : getSettings().getMany(OnlineFlashlistEventsTap.SETTINGS_KEY_FLASHLIST_ROOT)) {
			registerTap(new OnlineFlashlistEventsTap(this, flashlistsRootUrl));
		}

		// register the statements from file
		new BasicStructEplProvider().registerStatements(ep);
		new FileBasedEplProvider().registerStatements(ep);

		openTaps();
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

	private void setUpViews() {
		for (String viewName : settings.getMany("view")) {
			Object sink = instantiateComponent("sink", viewName);
			if (sink instanceof EventsSink) {
				views.add((EventsSink) sink);
			}
			if (sink instanceof LogSink) {
				LoadLogCollector.registerSink((LogSink) sink);
			}
		}
	}

	protected Collection<EventsTap> getTaps() {
		return this.taps;
	}

	protected boolean isInternalTimerEnabled() {
		return false;
	}

	protected void openTaps() {
		for (EventsTap et : taps) {
			et.openStreams();
		}
	}

	/**
	 * Inserts into the list of known taps this should depend on some
	 * configuration later on or start-up choice
	 */
	protected void registerTap(EventsTap tap) {
		taps.add(tap);
	}

	protected void setUpSOCKSProxy() {
		for (String key : new String[] { "socksProxyHost", "proxySet", "socksProxyPort" }) {
			if (settings.containsKey(key)) {
				System.getProperties().put(key, settings.get(key));
			}
		}
	}

}
