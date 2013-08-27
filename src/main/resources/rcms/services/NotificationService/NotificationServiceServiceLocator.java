/**
 * NotificationServiceServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package rcms.services.NotificationService;

public class NotificationServiceServiceLocator extends org.apache.axis.client.Service implements rcms.services.NotificationService.NotificationServiceService {

    public NotificationServiceServiceLocator() {
    }


    public NotificationServiceServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public NotificationServiceServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for NotificationService
    private java.lang.String NotificationService_address = "http://localhost:10000/rcms/services/NotificationService";

    public java.lang.String getNotificationServiceAddress() {
        return NotificationService_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String NotificationServiceWSDDServiceName = "NotificationService";

    public java.lang.String getNotificationServiceWSDDServiceName() {
        return NotificationServiceWSDDServiceName;
    }

    public void setNotificationServiceWSDDServiceName(java.lang.String name) {
        NotificationServiceWSDDServiceName = name;
    }

    public rcms.services.NotificationService.NotificationService getNotificationService() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(NotificationService_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getNotificationService(endpoint);
    }

    public rcms.services.NotificationService.NotificationService getNotificationService(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            rcms.services.NotificationService.NotificationServiceSoapBindingStub _stub = new rcms.services.NotificationService.NotificationServiceSoapBindingStub(portAddress, this);
            _stub.setPortName(getNotificationServiceWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setNotificationServiceEndpointAddress(java.lang.String address) {
        NotificationService_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (rcms.services.NotificationService.NotificationService.class.isAssignableFrom(serviceEndpointInterface)) {
                rcms.services.NotificationService.NotificationServiceSoapBindingStub _stub = new rcms.services.NotificationService.NotificationServiceSoapBindingStub(new java.net.URL(NotificationService_address), this);
                _stub.setPortName(getNotificationServiceWSDDServiceName());
                return _stub;
            }
        }
        catch (java.lang.Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        java.lang.String inputPortName = portName.getLocalPart();
        if ("NotificationService".equals(inputPortName)) {
            return getNotificationService();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://localhost:10000/rcms/services/NotificationService", "NotificationServiceService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://localhost:10000/rcms/services/NotificationService", "NotificationService"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("NotificationService".equals(portName)) {
            setNotificationServiceEndpointAddress(address);
        }
        else 
{ // Unknown Port Name
            throw new javax.xml.rpc.ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
        }
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(javax.xml.namespace.QName portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }

}
