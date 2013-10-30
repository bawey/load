package ch.cern.cms.load;

import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import ch.cern.cms.load.eplProviders.FileBasedEplProvider;
import ch.cern.cms.load.guis.DefaultGui;
import ch.cern.cms.load.guis.ExpertGui;
import ch.cern.cms.load.taps.AbstractEventsTap;
import ch.cern.cms.load.taps.flashlist.OfflineFlashlistEventsTap;

/**
 * Core components, singletons etc. should be initialized here. However, the setup of initial structure should be well separated from the
 * configuration-specific actions to enable unit-testing and reconfigurations.
 */

// try this for loading properties: inputFile = this.getClass().getClassLoader().getResourceAsStream("etc/trivia.epl")

public class Load {

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
		instance.attachViews();
	}

	private final EventProcessor ep = new EventProcessor();

	private final Set<ExpertGui> guis = new HashSet<ExpertGui>();

	private final Settings settings = new Settings();

	private final Set<AbstractEventsTap> taps = new HashSet<AbstractEventsTap>();

	private final FieldTypeResolver resolver = new FieldTypeResolver();

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

	/**
	 * this should allow to attach swing / net gui
	 */
	private void attachViews() {
		guis.add(new DefaultGui().attach(this));
	}

	/**
	 * Initializes the default application structure.
	 */
	private void defaultSetup() {
		// looks stupid - calls every known component to perform its setup depending on the config file's contents
		// place for dependency injection??

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

}
