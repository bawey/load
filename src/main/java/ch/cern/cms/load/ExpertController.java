package ch.cern.cms.load;

import java.text.RuleBasedCollator;
import java.util.HashSet;
import java.util.Set;

import ch.cern.cms.load.guis.DefaultGui;
import ch.cern.cms.load.guis.ExpertGui;
import ch.cern.cms.load.taps.EventsTap;
import ch.cern.cms.load.taps.flashlist.OfflineFlashlistEventsTap;

/**
 * Core components, singletons etc. should be initialized here. However, the
 * setup of initial structure should be well separated from the
 * configuration-specific actions to enable unit-testing and reconfigurations.
 */

public class ExpertController {

	private static ExpertController instance;

	public static ExpertController getInstance() {
		if (instance == null) {
			synchronized (ExpertController.class) {
				if (instance == null) {
					instance = new ExpertController();
				}
			}
		}
		return instance;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		instance = getInstance();
		instance.autoStart();

	}

	private final EventProcessor ep = new EventProcessor();

	private final Set<ExpertGui> guis = new HashSet<ExpertGui>();

	private final Settings settings = new Settings();

	private final Set<EventsTap> taps = new HashSet<EventsTap>();

	private final FieldTypeResolver resolver = new FieldTypeResolver();

	private final RulesBase rulesBase = new RulesBase(this);

	private ExpertController() {
		settings.put(Settings.KEY_RESOLVER, resolver);
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

	public RulesBase getRulesBase() {
		return rulesBase;
	}

	/**
	 * this should allow to attach swing / net gui
	 */
	private void attachViews() {
		guis.add(new DefaultGui().attach(this));
	}

	private void autoStart() {
		registerTap(new OfflineFlashlistEventsTap(this, "/home/bawey/Workspace/load/dmp/offlineFL/"));
		openTaps();
		registerStatements();
		attachViews();
	}

	private void consolePrint() {

	}

	private void openTaps() {
		for (EventsTap et : taps) {
			et.openStreams(ep);
		}
	}

	/**
	 * this should take into account some rules stored in db or config file
	 */
	private void registerStatements() {

	}

	/**
	 * Inserts into the list of known taps this should depend on some
	 * configuration later on or start-up choice
	 */
	public void registerTap(EventsTap tap) {
		taps.add(tap);
	}

}
