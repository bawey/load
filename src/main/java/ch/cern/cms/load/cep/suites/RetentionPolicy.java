package ch.cern.cms.load.cep.suites;

public class RetentionPolicy {
	public static String unique(String... args) {
		StringBuilder sb = new StringBuilder("std:,unique(");
		for (int i = 0; i < args.length; ++i) {
			sb.append(args[i]);
			if (i < args.length - 1) {
				sb.append(",");
			}
		}
		sb.append(")");
		return sb.toString();
	}
	public static final String KEEP_ALL = "win.keepall()";
	
}
