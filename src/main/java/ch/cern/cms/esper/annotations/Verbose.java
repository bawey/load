package ch.cern.cms.esper.annotations;

/**
 * @author Tomasz Bawej Annotation to be used with EPL statements supposed to produce output for the user. TODO: complete doc
 */
public @interface Verbose {
	/**
	 * Label for the results to be printed with
	 */
	String label() default "default";

	/**
	 * Statement result fields to be printed
	 */
	String[] fields() default {};

	/**
	 * Additional message to be printed with the statement output
	 */
	String extraNfo() default "";

	/**
	 * Setting to false should make the interactive views overwrite the output with statement results.
	 */
	boolean append() default true;
}
