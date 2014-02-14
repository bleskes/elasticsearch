/**
 * DMIServiceService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package adlex.delta.ws;

public interface DMIServiceService extends javax.xml.rpc.Service {
    public java.lang.String getDMIServiceAddress();

    public adlex.delta.ws.DMIService getDMIService() throws javax.xml.rpc.ServiceException;

    public adlex.delta.ws.DMIService getDMIService(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
}
