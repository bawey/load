package ch.cern.cms.load.taps;

import ch.cern.cms.load.EventProcessor;
import ch.cern.cms.load.Load;
import ch.cern.cms.load.taps.flashlist.OfflineFlashlistEventsTap;
import ch.cern.cms.load.taps.flashlist.OnlineFlashlistEventsTap;

public abstract class AbstractEventsTap {

	protected Runnable job = new Runnable() {
		@Override
		public void run() {
			throw new RuntimeException("EventsTap: " + this.getClass().getSimpleName() + " doesn't provide a valid job Runnable!");
		}
	};
	protected final EventProcessor ep;
	protected final Load controller;
	protected final String path;

	protected AbstractEventsTap(Load expert, String path) {
		this.ep = expert.getEventProcessor();
		this.controller = expert;
		this.path = path;
		preRegistrationSetup(expert);
		registerEventTypes(expert);
	}

	/**
	 * allows setting things up before performing the full registration with expert
	 * {@link AbstractEventsTap#initWithExpert(Load)}
	 **/
	public abstract void preRegistrationSetup(Load expert);

	/** adds event definitions to configuration obtained via {@link Load} **/
	public abstract void registerEventTypes(Load expert);

	/** launches the Tap-specific job in a separate thread **/
	public final void openStreams() {
		new Thread(job).start();
	}


	@Deprecated
	public static void registerKnownOnlineTaps() {
		Load ec = Load.getInstance();
		for (String flRoot : ec.getSettings().getMany(OnlineFlashlistEventsTap.SETTINGS_KEY_FLASHLIST_ROOT))
			ec.registerTap(new OnlineFlashlistEventsTap(ec, flRoot));
	}

	public static void setOfflineTapsPosition(long position) {
		for (AbstractEventsTap tap : Load.getInstance().getTaps()) {
			if (tap instanceof OfflineFlashlistEventsTap) {
				((OfflineFlashlistEventsTap) tap).setPosition(position);
			}
		}
	}
}
