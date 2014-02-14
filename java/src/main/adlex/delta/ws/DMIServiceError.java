/**
 * DMIServiceError.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package adlex.delta.ws;

public class DMIServiceError  implements java.io.Serializable {
    private boolean error;

    private java.lang.String[] errorDescriptions;

    private boolean info;

    private java.lang.String[] infoDescriptions;

    private boolean warning;

    private java.lang.String[] warningDescriptions;

    public DMIServiceError() {
    }

    public DMIServiceError(
           boolean error,
           java.lang.String[] errorDescriptions,
           boolean info,
           java.lang.String[] infoDescriptions,
           boolean warning,
           java.lang.String[] warningDescriptions) {
           this.error = error;
           this.errorDescriptions = errorDescriptions;
           this.info = info;
           this.infoDescriptions = infoDescriptions;
           this.warning = warning;
           this.warningDescriptions = warningDescriptions;
    }


    /**
     * Gets the error value for this DMIServiceError.
     * 
     * @return error
     */
    public boolean isError() {
        return error;
    }


    /**
     * Sets the error value for this DMIServiceError.
     * 
     * @param error
     */
    public void setError(boolean error) {
        this.error = error;
    }


    /**
     * Gets the errorDescriptions value for this DMIServiceError.
     * 
     * @return errorDescriptions
     */
    public java.lang.String[] getErrorDescriptions() {
        return errorDescriptions;
    }


    /**
     * Sets the errorDescriptions value for this DMIServiceError.
     * 
     * @param errorDescriptions
     */
    public void setErrorDescriptions(java.lang.String[] errorDescriptions) {
        this.errorDescriptions = errorDescriptions;
    }


    /**
     * Gets the info value for this DMIServiceError.
     * 
     * @return info
     */
    public boolean isInfo() {
        return info;
    }


    /**
     * Sets the info value for this DMIServiceError.
     * 
     * @param info
     */
    public void setInfo(boolean info) {
        this.info = info;
    }


    /**
     * Gets the infoDescriptions value for this DMIServiceError.
     * 
     * @return infoDescriptions
     */
    public java.lang.String[] getInfoDescriptions() {
        return infoDescriptions;
    }


    /**
     * Sets the infoDescriptions value for this DMIServiceError.
     * 
     * @param infoDescriptions
     */
    public void setInfoDescriptions(java.lang.String[] infoDescriptions) {
        this.infoDescriptions = infoDescriptions;
    }


    /**
     * Gets the warning value for this DMIServiceError.
     * 
     * @return warning
     */
    public boolean isWarning() {
        return warning;
    }


    /**
     * Sets the warning value for this DMIServiceError.
     * 
     * @param warning
     */
    public void setWarning(boolean warning) {
        this.warning = warning;
    }


    /**
     * Gets the warningDescriptions value for this DMIServiceError.
     * 
     * @return warningDescriptions
     */
    public java.lang.String[] getWarningDescriptions() {
        return warningDescriptions;
    }


    /**
     * Sets the warningDescriptions value for this DMIServiceError.
     * 
     * @param warningDescriptions
     */
    public void setWarningDescriptions(java.lang.String[] warningDescriptions) {
        this.warningDescriptions = warningDescriptions;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof DMIServiceError)) return false;
        DMIServiceError other = (DMIServiceError) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            this.error == other.isError() &&
            ((this.errorDescriptions==null && other.getErrorDescriptions()==null) || 
             (this.errorDescriptions!=null &&
              java.util.Arrays.equals(this.errorDescriptions, other.getErrorDescriptions()))) &&
            this.info == other.isInfo() &&
            ((this.infoDescriptions==null && other.getInfoDescriptions()==null) || 
             (this.infoDescriptions!=null &&
              java.util.Arrays.equals(this.infoDescriptions, other.getInfoDescriptions()))) &&
            this.warning == other.isWarning() &&
            ((this.warningDescriptions==null && other.getWarningDescriptions()==null) || 
             (this.warningDescriptions!=null &&
              java.util.Arrays.equals(this.warningDescriptions, other.getWarningDescriptions())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        _hashCode += (isError() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        if (getErrorDescriptions() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getErrorDescriptions());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getErrorDescriptions(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        _hashCode += (isInfo() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        if (getInfoDescriptions() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getInfoDescriptions());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getInfoDescriptions(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        _hashCode += (isWarning() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        if (getWarningDescriptions() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getWarningDescriptions());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getWarningDescriptions(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(DMIServiceError.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://ws.delta.adlex", "DMIServiceError"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("error");
        elemField.setXmlName(new javax.xml.namespace.QName("", "error"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("errorDescriptions");
        elemField.setXmlName(new javax.xml.namespace.QName("", "errorDescriptions"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/encoding/", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("info");
        elemField.setXmlName(new javax.xml.namespace.QName("", "info"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("infoDescriptions");
        elemField.setXmlName(new javax.xml.namespace.QName("", "infoDescriptions"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/encoding/", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("warning");
        elemField.setXmlName(new javax.xml.namespace.QName("", "warning"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("warningDescriptions");
        elemField.setXmlName(new javax.xml.namespace.QName("", "warningDescriptions"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/encoding/", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanSerializer(
            _javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanDeserializer(
            _javaType, _xmlType, typeDesc);
    }

}
