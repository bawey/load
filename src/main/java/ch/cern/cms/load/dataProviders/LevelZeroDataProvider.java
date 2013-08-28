package ch.cern.cms.load.dataProviders;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

import org.apache.axis.AxisFault;

import ch.cern.cms.load.configuration.Settings;

import parameterService.FunctionManagerParameterBean;
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
			stub = new ParameterControllerSoapBindingStub(new URL(settings.getEndpoint()), null);
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
}
