/**
 * ImportServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package ru.mos.dnevnik;

public class ImportServiceLocator extends org.apache.axis.client.Service implements ru.mos.dnevnik.ImportService {

    public ImportServiceLocator() {
    }


    public ImportServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public ImportServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for ImportServiceSoap
    private java.lang.String ImportServiceSoap_address = "http://31.13.60.189/services.dnevnik.mos.ru/ImportService.asmx";

    public java.lang.String getImportServiceSoapAddress() {
        return ImportServiceSoap_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String ImportServiceSoapWSDDServiceName = "ImportServiceSoap";

    public java.lang.String getImportServiceSoapWSDDServiceName() {
        return ImportServiceSoapWSDDServiceName;
    }

    public void setImportServiceSoapWSDDServiceName(java.lang.String name) {
        ImportServiceSoapWSDDServiceName = name;
    }

    public ru.mos.dnevnik.ImportServiceSoap getImportServiceSoap() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(ImportServiceSoap_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getImportServiceSoap(endpoint);
    }

    public ru.mos.dnevnik.ImportServiceSoap getImportServiceSoap(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            ru.mos.dnevnik.ImportServiceSoapStub _stub = new ru.mos.dnevnik.ImportServiceSoapStub(portAddress, this);
            _stub.setPortName(getImportServiceSoapWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setImportServiceSoapEndpointAddress(java.lang.String address) {
        ImportServiceSoap_address = address;
    }


    // Use to get a proxy class for ImportServiceSoap12
    private java.lang.String ImportServiceSoap12_address = "http://31.13.60.189/services.dnevnik.mos.ru/ImportService.asmx";

    public java.lang.String getImportServiceSoap12Address() {
        return ImportServiceSoap12_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String ImportServiceSoap12WSDDServiceName = "ImportServiceSoap12";

    public java.lang.String getImportServiceSoap12WSDDServiceName() {
        return ImportServiceSoap12WSDDServiceName;
    }

    public void setImportServiceSoap12WSDDServiceName(java.lang.String name) {
        ImportServiceSoap12WSDDServiceName = name;
    }

    public ru.mos.dnevnik.ImportServiceSoap getImportServiceSoap12() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(ImportServiceSoap12_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getImportServiceSoap12(endpoint);
    }

    public ru.mos.dnevnik.ImportServiceSoap getImportServiceSoap12(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            ru.mos.dnevnik.ImportServiceSoap12Stub _stub = new ru.mos.dnevnik.ImportServiceSoap12Stub(portAddress, this);
            _stub.setPortName(getImportServiceSoap12WSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setImportServiceSoap12EndpointAddress(java.lang.String address) {
        ImportServiceSoap12_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     * This service has multiple ports for a given interface;
     * the proxy implementation returned may be indeterminate.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (ru.mos.dnevnik.ImportServiceSoap.class.isAssignableFrom(serviceEndpointInterface)) {
                ru.mos.dnevnik.ImportServiceSoapStub _stub = new ru.mos.dnevnik.ImportServiceSoapStub(new java.net.URL(ImportServiceSoap_address), this);
                _stub.setPortName(getImportServiceSoapWSDDServiceName());
                return _stub;
            }
            if (ru.mos.dnevnik.ImportServiceSoap.class.isAssignableFrom(serviceEndpointInterface)) {
                ru.mos.dnevnik.ImportServiceSoap12Stub _stub = new ru.mos.dnevnik.ImportServiceSoap12Stub(new java.net.URL(ImportServiceSoap12_address), this);
                _stub.setPortName(getImportServiceSoap12WSDDServiceName());
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
        if ("ImportServiceSoap".equals(inputPortName)) {
            return getImportServiceSoap();
        }
        else if ("ImportServiceSoap12".equals(inputPortName)) {
            return getImportServiceSoap12();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "ImportService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "ImportServiceSoap"));
            ports.add(new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "ImportServiceSoap12"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("ImportServiceSoap".equals(portName)) {
            setImportServiceSoapEndpointAddress(address);
        }
        else 
if ("ImportServiceSoap12".equals(portName)) {
            setImportServiceSoap12EndpointAddress(address);
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
