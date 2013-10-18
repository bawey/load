package ch.cern.cms.load.taps.flashlist;

import ch.cern.cms.load.ExpertController;
import ch.cern.cms.load.taps.AbstractEventsTap;

/**
 * Nobody knows if anybody really needs it
 */

public abstract class AbstractFlashlistEventsTap extends AbstractEventsTap {
	public static final String PKEY_FIELD_TYPE = "PROPERTY_KEY_FIELD_NAME_TO_TYPE_MAP";

	protected AbstractFlashlistEventsTap(ExpertController expert, String path) {
		super(expert, path);
	}

}
