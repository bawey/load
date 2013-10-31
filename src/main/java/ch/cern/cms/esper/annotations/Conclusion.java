package ch.cern.cms.esper.annotations;

/**
 * Should denote a statement creating a valid conclusion stream. This could be
 * done without annotations, yet for the clarity's sake and against doing things
 * behind the scenes, we'll go with Annotation
 */

public @interface Conclusion {
	String streamName();
}
