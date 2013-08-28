package ch.cern.cms.sentry;



public class ParameterReceiver {
//	public static final String[] IDS = { "http://pcbawejdesktop.cern.ch:10000/urn:rcms-fm:fullpath=/bawey/test/testLEVELZERO,group=levelZeroFM,owner=bawey" };
//	public static final String endpoint = "http://pcbawejdesktop.cern.ch:10000/rcms/services/ParameterController";
//
//	public static final void main(String[] args) throws IOException {
//		ParameterControllerSoapBindingStub stub = new ParameterControllerSoapBindingStub(new URL(endpoint), null);
//		FunctionManagerParameterBean[] someParamBeans = stub.getParameter(IDS);
//		if (someParamBeans != null) {
//			if (someParamBeans.length > 0) {
//				exportToXml(someParamBeans);
//				// for (FunctionManagerParameterBean bean : someParamBeans) {
//				// if (bean.getName() != null &&
//				// bean.getName().startsWith("SUBSYSTEMS_PARAMETERS_MAP")) {
//				// // System.out.println(prettyPrintBean(bean, 0));
//				// // System.out.println("Found the bean of interest. Type: "
//				// // + bean.getType() + ", Value: " + bean.getValue());
//				// // System.out.println("New Method Test:");
//				// // System.out.println(Infirmary.parameterBeanToJson(bean));
//				// } else if (bean.getName() != null &&
//				// bean.getName().endsWith("_RUN_KEYS")) {
//				// // System.out.println("RUN KEYS STRING OF LENGTH: " +
//				// // bean.getValue().length());
//				// // System.out.println(prettyPrintBean(bean, 0));
//				// // System.out.println("retrieved from JSON: " +
//				// // flattenList(extractFromJsonArray(bean.getValue())));
//				// } else if (bean.getName().contains("DAQ_SUBMITTED_PARAMS")) {
//				// System.out.println(bean.getName() + " - " + bean.getValue());
//				// }
//				// }
//			} else {
//				System.out.println("just an empty array");
//			}
//		} else {
//			System.out.println("just a null");
//		}
//	}
//
//	public static final String[] xmlSuffixes = { "CC_PARAM_MAP", "PASSED_PARAMS", "APPLIED_PARAMS", "STATIC", "STATUS" };
//
//	private static boolean isInteresting(String paramName) {
//		for (String s : xmlSuffixes) {
//			if (paramName.contains(s)) {
//				// return true;
//			}
//			if (paramName.contains("DAQ_SUBMITTED")) {
//				return true;
//			}
//		}
//		return false;
//	}
//
//	private static void exportToXml(FunctionManagerParameterBean[] paramBeans) throws IOException {
//		File output = new File("/tmp/fmParams.xml");
//		BufferedWriter bw = new BufferedWriter(new FileWriter(output));
//		bw.write("<xml>");
//		List<FunctionManagerParameterBean> beansToPrint = new ArrayList<FunctionManagerParameterBean>();
//		for (FunctionManagerParameterBean bean : paramBeans) {
//			if (isInteresting(bean.getName())) {
//				beansToPrint.add(bean);
//			}
//		}
//		Collections.sort(beansToPrint, new Comparator<FunctionManagerParameterBean>() {
//			@Override
//			public int compare(FunctionManagerParameterBean arg0, FunctionManagerParameterBean arg1) {
//				return arg0.getName().compareTo(arg1.getName());
//			}
//		});
//		for (FunctionManagerParameterBean bean : beansToPrint) {
//			bw.write(bean2XML(bean, 1));
//		}
//		bw.write("</xml>");
//		bw.flush();
//		bw.close();
//	}
//
//	private static String bean2XML(ParameterBean bean, int indent) {
//		StringBuilder sb = new StringBuilder();
//		for (int i = 0; i < indent; ++i) {
//			sb.append("\t");
//		}
//		sb.append("<param name=\"");
//		sb.append(bean.getName());
//		sb.append("\">");
//		if (bean.getType().contains("VectorT")) {
//			for (ParameterBean parBean : bean.getParameters()) {
//				sb.append(bean2XML(parBean, indent + 1));
//			}
//		} else if (bean.getType().contains("MapT")) {
//			ParameterBean keyBean = bean.getParameters()[0];
//			ParameterBean valBean = bean.getParameters()[1];
//			sb.append(keyValBeans2XML(keyBean, valBean, indent + 1));
//		} else {
//
//			if (bean.getName().contains("HTML")) {
//				sb.append("some html chunk");
//			} else if (bean.getName().contains("FED_NAME_MAP")) {
//				sb.append("long, long stuff");
//			} else {
//				sb.append(bean.getValue());
//			}
//			return sb.append("</param>").toString();
//		}
//		sb.append("\n");
//		for (int i = 0; i < indent; ++i) {
//			sb.append("\t");
//		}
//		return sb.append("</param>").toString();
//	}
//
//	private static final String keyValBeans2XML(ParameterBean keyBean, ParameterBean valBean, int indent) {
//		StringBuilder sb = new StringBuilder();
//
//		for (int i = 0; i < keyBean.getParameters().length; ++i) {
//			sb.append("<param name=\"").append(keyBean.getParameters()[i].getValue()).append("\">");
//			ParameterBean bean = valBean.getParameters()[i];
//			if (bean.getType().contains("Map") || bean.getType().contains("Vector")) {
//				sb.append("\n");
//				sb.append(bean2XML(bean, indent + 1));
//				sb.append("\n");
//			} else {
//				sb.append(bean.getValue());
//			}
//			sb.append("</param>");
//		}
//		return sb.toString();
//	}
//
//	private static final String prettyPrintBean(ParameterBean bean, int level) {
//		if (bean.getType().equalsIgnoreCase("rcms.fm.fw.parameter.type.MapT")) {
//			System.out.println(prettyPrintMapBean(bean, level));
//		}
//		StringBuilder sb = new StringBuilder("ParameterBean >> ");
//		for (int i = 0; i < level; ++i) {
//			sb.append("- - ");
//		}
//		sb.append(bean.getType()).append(": ").append(bean.getName()).append("-").append(bean.getValue());
//		if (bean.getParameters() != null && bean.getParameters().length > 0) {
//			for (ParameterBean pb : bean.getParameters()) {
//				sb.append("\n ").append(prettyPrintBean(pb, level + 1));
//			}
//		}
//
//		return sb.toString();
//	}
//
//	private static final boolean isParamMap(ParameterBean bean) {
//		if (bean.getParameters() != null) {
//			// System.out.println(bean.getParameters()[0].getType() + ", " +
//			// bean.getParameters()[0].getName());
//		}
//		return bean.getParameters() != null && bean.getParameters().length == 2 && bean.getParameters()[0].getName().equals("MAP_KEYS")
//				&& bean.getParameters()[1].getName().equals("MAP_VALUES");
//	}
//
//	private static final Vector<Object> reconstructVector(ParameterBean bean) {
//		Vector<Object> v = new Vector<Object>();
//		for (int i = 0; i < bean.getParameters().length; ++i) {
//			ParameterBean valBean = bean.getParameters()[i];
//			if (isParamMap(valBean)) {
//				v.add(reconstructMap(valBean));
//			} else if (valBean.getType().contains("VectorT")) {
//				v.add(reconstructVector(valBean));
//			} else {
//				v.add(valBean.getValue());
//			}
//
//		}
//		return v;
//	}
//
//	private static final Map<Object, Object> reconstructMap(ParameterBean bean) {
//		if (!bean.getType().equalsIgnoreCase("rcms.fm.fw.parameter.type.MapT")) {
//			return null;
//		}
//		Map<Object, Object> map = new HashMap<Object, Object>();
//		if (bean != null && bean.getParameters() != null) {
//			for (int i = 0; i < bean.getParameters()[0].getParameters().length; ++i) {
//				ParameterBean valBean = bean.getParameters()[1].getParameters()[i];
//				if (isParamMap(valBean)) {
//					map.put(bean.getParameters()[0].getParameters()[i].getValue(), reconstructMap(valBean));
//				} else if (valBean.getType().contains("VectorT")) {
//					map.put(bean.getParameters()[0].getParameters()[i].getValue(), reconstructVector(valBean));
//				} else {
//					map.put(bean.getParameters()[0].getParameters()[i].getValue(), valBean.getValue());
//				}
//			}
//		} else {
//			System.out.println("Bean has no parameters!");
//		}
//		return map;
//	}
//
//	private static final String prettyPrintMapBean(ParameterBean bean, int level) {
//		Map<Object, Object> map = reconstructMap(bean);
//		return map.toString();
//	}
//
//	private static List<String> extractFromJsonArray(String json) {
//		List<String> result = new LinkedList<String>();
//		Pattern p = Pattern.compile("\"[^\"]*\"");
//		Matcher matcher = p.matcher(json);
//		while (matcher.find()) {
//			result.add(matcher.group());
//		}
//		return result;
//	}
//
//	private static String flattenList(List<? extends Object> stuff) {
//		StringBuilder sb = new StringBuilder("#");
//		for (Object s : stuff) {
//			sb.append(s).append("#");
//		}
//		return sb.toString();
//	}
//
//	public static final String KNOWN_SUBSYSTEMS[] = new String[] { "PIXEL", "TRACKER", "ES", "ECAL", "HCAL", "HFLUMI", "CASTOR", "DT",
//			"CSC", "RPC", "TRG", "SCAL", "LTC", "DAQ", "DQM", "DCS", "EFED", "TRACKER_EFED", "ECAL_EFED", "HCAL_EFED", "DT_EFED",
//			"CSC_EFED", "RPC_EFED", "TRG_EFED", "COW" };
//
//	public static final String KNOWN_PARAS[] = new String[] { "_STATE", "_COMPLETION", "_ACTION_MSG", "_ERROR_MSG", "_FEDS", "_SAVEDFEDS",
//			"_RUN_KEYS", "_RUN_KEY", "_URL", "_RECYCLE_HTML", "_RECONFIGURE_TOOLTIP", "_RECYCLE_TOOLTIP", "_GUI_ACCESS_ALLOWED",
//			"_GUI_ACCESS_ALLOWED_HTML", "_HAS_OTHER_PARENT", "_PARENT", "_PREVENTIVE_SOFT_ERROR_RECOVERY" };
}
