package ch.cern.cms.load;


public abstract class EventsTap {

	protected Runnable job = new Runnable() {
		@Override
		public void run() {
			throw new RuntimeException("EventsTap: " + this.getClass().getSimpleName() + " doesn't provide a valid job Runnable!");
		}
	};
	protected final EventProcessor ep;
	protected final Load controller;
	protected final String path;

	protected EventsTap(Load expert, String path) {
		this.ep = expert.getEventProcessor();
		this.controller = expert;
		this.path = path;
		preRegistrationSetup(expert);
		registerEventTypes(expert);
	}

	/**
	 * allows setting things up before performing the full registration with expert
	 * {@link EventsTap#initWithExpert(Load)}
	 **/
	public abstract void preRegistrationSetup(Load expert);

	/** adds event definitions to configuration obtained via {@link Load} **/
	protected abstract void registerEventTypes(Load expert);

	/** launches the Tap-specific job in a separate thread **/
	public final void openStreams() {
		new Thread(job).start();
	}
}
