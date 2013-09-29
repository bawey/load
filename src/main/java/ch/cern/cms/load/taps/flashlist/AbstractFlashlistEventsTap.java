package ch.cern.cms.load.taps.flashlist;

import ch.cern.cms.load.ExpertController;
import ch.cern.cms.load.taps.EventsTap;

public abstract class AbstractFlashlistEventsTap extends EventsTap {
	public static final String PKEY_FIELD_TYPE = "PROPERTY_KEY_FIELD_NAME_TO_TYPE_MAP";

	protected AbstractFlashlistEventsTap() {
		super();
	}

	protected AbstractFlashlistEventsTap(ExpertController expert) {
		super(expert);
	}

}
