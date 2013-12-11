package ch.cern.cms.load.views;

import java.lang.annotation.Annotation;

import ch.cern.cms.esper.annotations.Verbose;

import com.espertech.esper.client.EPStatement;

public class VerboseAttributes {

	public final String extrMsg;
	public final Boolean append;
	public final String[] streamPath;
	public final String[] fields;
	public final String label;

	public VerboseAttributes(EPStatement statement) {
		for (Annotation ann : statement.getAnnotations()) {
			if (ann.annotationType().equals(Verbose.class)) {
				label = ((Verbose) ann).label();
				fields = ((Verbose) ann).fields();
				extrMsg = ((Verbose) ann).extraNfo();
				append = ((Verbose) ann).append();
				streamPath = ((Verbose) ann).streamPath();
				return;
			}
		}
		label = null;
		fields = null;
		extrMsg = null;
		append = null;
		streamPath = null;
	}
}
