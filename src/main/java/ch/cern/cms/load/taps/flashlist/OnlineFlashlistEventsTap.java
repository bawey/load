package ch.cern.cms.load.taps.flashlist;

import ch.cern.cms.load.ExpertController;

public class OnlineFlashlistEventsTap extends AbstractFlashlistEventsTap {

	private OnlineFlashlistEventsTap(ExpertController expert, String path) {
		super(expert, path);
		job = new Runnable() {
			@Override
			public void run() {
				// fill it in
			}
		};
	}

	/** need to connect to server, bla bla bla **/
	@Override
	public void preRegistrationSetup() {

	}

	@Override
	public void registerEventTypes() {
		// TODO Auto-generated method stub
	}

}
