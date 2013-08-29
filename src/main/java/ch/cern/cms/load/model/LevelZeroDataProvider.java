package ch.cern.cms.load.model;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.axis.AxisFault;

import ch.cern.cms.load.configuration.Settings;

import parameterService.FunctionManagerParameterBean;
import parameterService.ParameterBean;
import parameterService.ParameterControllerSoapBindingStub;

/**
 * @author Tomasz Bawej This class will contact the WS and return something.
 *         Preferably something that can be stored and used by DummyProvider in
 *         the future.
 */
public class LevelZeroDataProvider {

	private static LevelZeroDataProvider instance;

	public static LevelZeroDataProvider getInstance() {
		if (instance == null) {
			synchronized (LevelZeroDataProvider.class) {
				if (instance == null) {
					instance = new LevelZeroDataProvider();
				}
			}
		}
		return instance;
	}

	private LevelZeroDataProvider() {
		try {
			stub = new ParameterControllerSoapBindingStub(new URL(settings.getParametersEndpoint()), null);
		} catch (Throwable e) {
			throw new RuntimeException("Well, that mechanism should be there for recovery but....", e);
		}
	}

	/** instance code **/

	private Settings settings = Settings.getInstance();
	private ParameterControllerSoapBindingStub stub = null;

	public FunctionManagerParameterBean[] getRawData() throws MalformedURLException, RemoteException {

		return stub.getParameter(settings.getIDS());
	}

	public Map<String, Object> getAsMap() throws MalformedURLException, RemoteException {
		Map<String, Object> map = new HashMap<String, Object>();
		FunctionManagerParameterBean[] beans = getRawData();
		for (FunctionManagerParameterBean bean : beans) {
			if (!bean.getName().contains("_HTML")) {
				map.put(bean.getName(), objectFromBean(bean));
			}
		}
		return map;
	}

	/**
	 * UTILS FOR CONVERTING PARAMETER_BEAN INTO A MAP
	 */
	private final Vector<Object> reconstructVector(ParameterBean bean) {
		Vector<Object> v = new Vector<Object>();
		for (int i = 0; i < bean.getParameters().length; ++i) {
			ParameterBean valBean = bean.getParameters()[i];
			if (isParamMap(valBean)) {
				v.add(reconstructMap(valBean));
			} else if (isParamMap(valBean)) {
				v.add(reconstructVector(valBean));
			} else {
				v.add(valBean.getValue());
			}

		}
		return v;
	}

	private final Map<Object, Object> reconstructMap(ParameterBean bean) {
		if (!bean.getType().equalsIgnoreCase("rcms.fm.fw.parameter.type.MapT")) {
			return null;
		}
		Map<Object, Object> map = new HashMap<Object, Object>();
		if (bean != null && bean.getParameters() != null) {
			for (int i = 0; i < bean.getParameters()[0].getParameters().length; ++i) {
				ParameterBean valBean = bean.getParameters()[1].getParameters()[i];
				if (isParamMap(valBean)) {
					map.put(bean.getParameters()[0].getParameters()[i].getValue(), reconstructMap(valBean));
				} else if (isParamVector(valBean)) {
					map.put(bean.getParameters()[0].getParameters()[i].getValue(), reconstructVector(valBean));
				} else {
					map.put(bean.getParameters()[0].getParameters()[i].getValue(), valBean.getValue());
				}
			}
		} else {
			System.out.println("Bean has no parameters!");
		}
		return map;
	}

	private boolean isParamMap(ParameterBean bean) {
		return bean.getParameters() != null && bean.getParameters().length == 2 && bean.getParameters()[0].getName().equals("MAP_KEYS")
				&& bean.getParameters()[1].getName().equals("MAP_VALUES");
	}

	private boolean isParamVector(ParameterBean bean) {
		return bean.getType().contains("VectorT");
	}

	private Object objectFromBean(FunctionManagerParameterBean bean) {
		if (bean.getType().contains("BinaryStringT")) {
			return bean.getValue();
		} else if (bean.getType().contains("StringT")) {
			return bean.getValue();
		} else if (bean.getType().contains("IntegerT")) {
			return Integer.parseInt(bean.getValue());
		} else if (bean.getType().contains("LongT")) {
			return Long.parseLong(bean.getValue());
		} else if (bean.getType().contains("DoubleT")) {
			return Double.parseDouble(bean.getValue());
		} else if (isParamMap(bean)) {
			return reconstructMap(bean);
		} else if (isParamVector(bean)) {
			return reconstructVector(bean);
		}
		return null;
	}

}
