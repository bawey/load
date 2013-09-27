package ch.cern.cms.load;

import java.util.HashSet;
import java.util.Set;

import ch.cern.cms.load.configuration.Settings;
import ch.cern.cms.load.eventProcessing.EventProcessor;
import ch.cern.cms.load.guis.DefaultGui;
import ch.cern.cms.load.guis.ExpertGui;
import ch.cern.cms.load.taps.EventsTap;

/**
 * Core components, singletons etc. should be initialized here. However, the
 * setup of initial structure should be well separated from the
 * configuration-specific actions to enable unit-testing and reconfigurations.
 */

public class ExpertController {

	public Settings getSettings() {
		return settings;
	}

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

	private final Settings settings = Settings.getInstance();

	private final EventProcessor ep = EventProcessor.getInstance();

	private final Set<ExpertGui> guis = new HashSet<ExpertGui>();

	private final Set<EventsTap> taps = new HashSet<EventsTap>();

	private ExpertController() {

	}

	private void autoStart() {
		registerTaps();
		tapsSetup();
		openTaps();
		registerStatements();
		attachViews();
	}

	/**
	 * this should allow to attach swing / net gui
	 */
	private void attachViews() {
		guis.add(new DefaultGui().attach(this));
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
	 * Inserts into the list of known taps
	 * this should depend on some configuration later on or start-up choice
	 */
	private void registerTaps() {
		// taps.add(null);
		// taps.add(null);
	}

	private void tapsSetup() {
		for (EventsTap et : taps) {
			et.defineProperties(this);
			et.registerEventTypes(ep);
		}
	}

}
