package ch.cern.cms.load;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import ch.cern.cms.load.eplProviders.FileBasedEplProvider;
import ch.cern.cms.load.guis.DefaultGui;
import ch.cern.cms.load.guis.ExpertGui;
import ch.cern.cms.load.taps.AbstractEventsTap;

/**
 * Core components, singletons etc. should be initialized here. However, the setup of initial structure should be well separated from the
 * configuration-specific actions to enable unit-testing and reconfigurations.
 */

// try this for loading properties: inputFile = this.getClass().getClassLoader().getResourceAsStream("etc/trivia.epl")

public class Load {

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
		instance.defaultSetup();
	}

	private final EventProcessor ep = new EventProcessor();

	private final Set<ExpertGui> guis = new HashSet<ExpertGui>();

	private final Settings settings = new Settings();

	private final Set<AbstractEventsTap> taps = new HashSet<AbstractEventsTap>();

	private final FieldTypeResolver resolver = new FieldTypeResolver();

	private final Set<LoadView> views = new HashSet<LoadView>();

	private Load() {
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
		AbstractEventsTap.registerKnownOfflineTaps();
		AbstractEventsTap.registerKnownOnlineTaps();
		// register the statements from file
		new FileBasedEplProvider().registerStatements(ep);

		openTaps();
	}

	public void openTaps() {
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
			System.out.println("wow, a view: " + viewName);
			ClassLoader ldr = this.getClass().getClassLoader();
			try {
				String interfaceName = LoadView.class.getCanonicalName();
				LoadView newView = (LoadView) ldr.loadClass(interfaceName.substring(0, interfaceName.lastIndexOf('.') + 1) + "views." + viewName).newInstance();
				views.add(newView);

			} catch (Exception e) {
				logger.error("Failed to register the view: " + viewName, e);
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
	}
}
