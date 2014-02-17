/**
 * DMIServiceServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package adlex.delta.ws;

public class DMIServiceServiceLocator extends org.apache.axis.client.Service implements adlex.delta.ws.DMIServiceService {

    public DMIServiceServiceLocator() {
    }


    public DMIServiceServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public DMIServiceServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for DMIService
    private java.lang.String DMIService_address = "http://localhost/services/DMIService";

    public java.lang.String getDMIServiceAddress() {
        return DMIService_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String DMIServiceWSDDServiceName = "DMIService";

    public java.lang.String getDMIServiceWSDDServiceName() {
        return DMIServiceWSDDServiceName;
    }

    public void setDMIServiceWSDDServiceName(java.lang.String name) {
        DMIServiceWSDDServiceName = name;
    }

    public adlex.delta.ws.DMIService getDMIService() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(DMIService_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getDMIService(endpoint);
    }

    public adlex.delta.ws.DMIService getDMIService(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            adlex.delta.ws.DMIServiceSoapBindingStub _stub = new adlex.delta.ws.DMIServiceSoapBindingStub(portAddress, this);
            _stub.setPortName(getDMIServiceWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setDMIServiceEndpointAddress(java.lang.String address) {
        DMIService_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (adlex.delta.ws.DMIService.class.isAssignableFrom(serviceEndpointInterface)) {
                adlex.delta.ws.DMIServiceSoapBindingStub _stub = new adlex.delta.ws.DMIServiceSoapBindingStub(new java.net.URL(DMIService_address), this);
                _stub.setPortName(getDMIServiceWSDDServiceName());
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
        if ("DMIService".equals(inputPortName)) {
            return getDMIService();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://ws.delta.adlex", "DMIServiceService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://ws.delta.adlex", "DMIService"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("DMIService".equals(portName)) {
            setDMIServiceEndpointAddress(address);
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
