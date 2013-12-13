package ch.cern.cms.esper.annotations;

public @interface DaqStateMask {
	String[] states() default {};
}
