package ch.cern.cms.load.taps;

import ch.cern.cms.load.EventProcessor;
import ch.cern.cms.load.ExpertController;
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
	protected final ExpertController controller;
	protected final String path;

	protected AbstractEventsTap(ExpertController expert, String path) {
		this.ep = expert.getEventProcessor();
		this.controller = expert;
		this.path = path;
		preRegistrationSetup();
		registerEventTypes();
	}

	/**
	 * allows setting things up before performing the full registration with expert
	 * {@link AbstractEventsTap#initWithExpert(ExpertController)}
	 **/
	public abstract void preRegistrationSetup();

	/** adds event definitions to configuration obtained via {@link ExpertController} **/
	public abstract void registerEventTypes();

	/** launches the Tap-specific job in a separate thread **/
	public final void openStreams() {
		new Thread(job).start();
	}

	public static void registerKnownOfflineTaps() {
		ExpertController ec = ExpertController.getInstance();
		for (String dir : ec.getSettings().getMany(OfflineFlashlistEventsTap.SETTINGS_KEY_FLASHLIST_DIR)) {
			ec.registerTap(new OfflineFlashlistEventsTap(ec, dir));
		}
	}

	public static void registerKnownOnlineTaps() {
		ExpertController ec = ExpertController.getInstance();
		for (String flRoot : ec.getSettings().getMany(OnlineFlashlistEventsTap.SETTINGS_KEY_FLASHLIST_ROOT))
			ec.registerTap(new OnlineFlashlistEventsTap(ec, flRoot));
	}

	public static void setOfflineTapsPace(double pace) {
		for (AbstractEventsTap tap : ExpertController.getInstance().getTaps()) {
			if (tap instanceof OfflineFlashlistEventsTap) {
				((OfflineFlashlistEventsTap) tap).setPace(pace);
			}
		}
	}

	public static void setOfflineTapsPosition(long position) {
		for (AbstractEventsTap tap : ExpertController.getInstance().getTaps()) {
			if (tap instanceof OfflineFlashlistEventsTap) {
				((OfflineFlashlistEventsTap) tap).setPosition(position);
			}
		}
	}
}
