package ch.cern.cms.load;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import ch.cern.cms.load.eplProviders.BasicStructEplProvider;
import ch.cern.cms.load.eplProviders.FileBasedEplProvider;
import ch.cern.cms.load.guis.ExpertGui;
import ch.cern.cms.load.taps.AbstractEventsTap;
import ch.cern.cms.load.taps.flashlist.DataBaseFlashlistEventsTap;
import ch.cern.cms.load.taps.flashlist.OfflineFlashlistEventsTap;
import ch.cern.cms.load.taps.flashlist.OfflineFlashlistEventsTap2;
import ch.cern.cms.load.timers.SimplePlaybackTimer;

/**
 * Core components, singletons etc. should be initialized here. However, the setup of initial structure should be well separated from the
 * configuration-specific actions to enable unit-testing and reconfigurations.
 */

// try this for loading properties: inputFile = this.getClass().getClassLoader().getResourceAsStream("etc/trivia.epl")

public class Load {

	private Class<?> dummy = SimplePlaybackTimer.class;
	private static final Logger logger = Logger.getLogger(Load.class);
	private static Load instance;

	public static Load getInstance() {
		if (instance == null) {
			synchronized (Load.class) {
				if (instance == null) {
					instance = new Load();
				}
			}
		}
		return instance;
	}

	/**
	 * @param args
	 */
	public static final void main(String[] args) {
		instance = getInstance();
		if (instance.settings.getProperty(DataBaseFlashlistEventsTap.KEY_DB_MODE, "read").equalsIgnoreCase("write")) {
			DbPumper.main(args);
		} else {
			instance.defaultSetup();
		}
	}

	private final Settings settings;

	private final EventProcessor ep;

	private final Set<ExpertGui> guis = new HashSet<ExpertGui>();

	private final Set<AbstractEventsTap> taps = new HashSet<AbstractEventsTap>();

	private final FieldTypeResolver resolver;

	private final Set<LoadView> views = new HashSet<LoadView>();

	private EPTimer timer = null;

	private Load() {
		// settings have to go first!
		settings = new Settings();
		ep = new EventProcessor(this);
		resolver = new FieldTypeResolver(this);
		settings.put(Settings.KEY_RESOLVER, resolver);
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

	public Set<LoadView> getViews() {
		return views;
	}

	public EPTimer getTimer() {
		return timer;
	}

	/**
	 * Initializes the default application structure.
	 */
	private void defaultSetup() {
		// looks stupid - calls every known component to perform its setup depending on the config file's contents
		// place for dependency injection??

		temporaryMethodToSetUpResolverTypes();

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
		AbstractEventsTap.registerKnownOnlineTaps();
		setUpTimer();
		// register the statements from file
		new BasicStructEplProvider().registerStatements(ep);
		new FileBasedEplProvider().registerStatements(ep);

		openTaps();
	}

	public void openTaps() {
		if (timer != null) {
			timer.start();
		}
		for (AbstractEventsTap et : taps) {
			et.openStreams();
		}
	}

	/**
	 * Inserts into the list of known taps this should depend on some configuration later on or start-up choice
	 */
	public void registerTap(AbstractEventsTap tap) {
		taps.add(tap);
	}

	public Collection<AbstractEventsTap> getTaps() {
		return this.taps;
	}

	private void setUpSOCKSProxy() {
		for (String key : new String[] { "socksProxyHost", "proxySet", "socksProxyPort" }) {
			System.getProperties().put(key, settings.get(key));
		}
	}

	private void setUpViews() {
		for (String viewName : settings.getMany("view")) {
			views.add((LoadView) instantiateComponent("view", viewName));
		}
	}

	private void setUpTimer() {
		if (settings.containsKey(Settings.KEY_TIMER)) {
			this.timer = (EPTimer) instantiateComponent(Settings.KEY_TIMER, settings.getProperty(Settings.KEY_TIMER));
			if (settings.containsKey(Settings.KEY_TIMER_PACE)) {
				timer.setPace(Double.parseDouble(settings.getProperty(Settings.KEY_TIMER_PACE)));
			}
			if (settings.containsKey(Settings.KEY_TIMER_STEP)) {
				timer.setStepSize(Long.parseLong(settings.getProperty(Settings.KEY_TIMER_STEP)));
			}
		}
	}

	private void temporaryMethodToSetUpResolverTypes() {
		getResolver().setFieldType("deltaT", Double.class);
		getResolver().setFieldType("deltaN", Double.class);
		getResolver().setFieldType("fifoAlmostFullCnt", Long.class);
		getResolver().setFieldType("fractionBusy", Double.class);
		getResolver().setFieldType("fractionWarning", Double.class);
		getResolver().setFieldType("clockCount", Double.class);
		getResolver().setFieldType("linkNumber", Integer.class);
		getResolver().setFieldType("slotNumber", Integer.class);
		getResolver().setFieldType("geoslot", Integer.class);
		getResolver().setFieldType("io", Integer.class);
		getResolver().setFieldType("epMacroStateInt", List.class);
		getResolver().setFieldType("nbProcessed", Long.class);
		getResolver().setFieldType("bxNumber", Long.class);
		getResolver().setFieldType("triggerNumber", Long.class);
		getResolver().setFieldType("FEDSourceId", Integer.class);
		getResolver().setFieldType("timestamp", Date.class);
		getResolver().setFieldType("lastEVMtimestamp", Date.class);
		getResolver().setFieldType("streamNames", String[].class);
		getResolver().setFieldType("ratePerStream", Double[].class);
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

	public double getPace() {
		if (timer != null) {
			return timer.getPace();
		} else {
			return 1;
		}
	}

}
