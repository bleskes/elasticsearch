/**
 * DMIData.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package adlex.delta.ws;

public class DMIData  implements java.io.Serializable {
    private java.lang.String[] columnHeader;

    private adlex.delta.ws.DMIServiceError[] dmiServiceError;

    private java.lang.String[][] formattedData;

    private java.lang.Double[][] rawData;

    private java.lang.Long timeBegin;

    private java.lang.Long timeEnd;

    private boolean timeout;

    private long timeoutValue;

    public DMIData() {
    }

    public DMIData(
           java.lang.String[] columnHeader,
           adlex.delta.ws.DMIServiceError[] dmiServiceError,
           java.lang.String[][] formattedData,
           java.lang.Double[][] rawData,
           java.lang.Long timeBegin,
           java.lang.Long timeEnd,
           boolean timeout,
           long timeoutValue) {
           this.columnHeader = columnHeader;
           this.dmiServiceError = dmiServiceError;
           this.formattedData = formattedData;
           this.rawData = rawData;
           this.timeBegin = timeBegin;
           this.timeEnd = timeEnd;
           this.timeout = timeout;
           this.timeoutValue = timeoutValue;
    }


    /**
     * Gets the columnHeader value for this DMIData.
     * 
     * @return columnHeader
     */
    public java.lang.String[] getColumnHeader() {
        return columnHeader;
    }


    /**
     * Sets the columnHeader value for this DMIData.
     * 
     * @param columnHeader
     */
    public void setColumnHeader(java.lang.String[] columnHeader) {
        this.columnHeader = columnHeader;
    }


    /**
     * Gets the dmiServiceError value for this DMIData.
     * 
     * @return dmiServiceError
     */
    public adlex.delta.ws.DMIServiceError[] getDmiServiceError() {
        return dmiServiceError;
    }


    /**
     * Sets the dmiServiceError value for this DMIData.
     * 
     * @param dmiServiceError
     */
    public void setDmiServiceError(adlex.delta.ws.DMIServiceError[] dmiServiceError) {
        this.dmiServiceError = dmiServiceError;
    }


    /**
     * Gets the formattedData value for this DMIData.
     * 
     * @return formattedData
     */
    public java.lang.String[][] getFormattedData() {
        return formattedData;
    }


    /**
     * Sets the formattedData value for this DMIData.
     * 
     * @param formattedData
     */
    public void setFormattedData(java.lang.String[][] formattedData) {
        this.formattedData = formattedData;
    }


    /**
     * Gets the rawData value for this DMIData.
     * 
     * @return rawData
     */
    public java.lang.Double[][] getRawData() {
        return rawData;
    }


    /**
     * Sets the rawData value for this DMIData.
     * 
     * @param rawData
     */
    public void setRawData(java.lang.Double[][] rawData) {
        this.rawData = rawData;
    }


    /**
     * Gets the timeBegin value for this DMIData.
     * 
     * @return timeBegin
     */
    public java.lang.Long getTimeBegin() {
        return timeBegin;
    }


    /**
     * Sets the timeBegin value for this DMIData.
     * 
     * @param timeBegin
     */
    public void setTimeBegin(java.lang.Long timeBegin) {
        this.timeBegin = timeBegin;
    }


    /**
     * Gets the timeEnd value for this DMIData.
     * 
     * @return timeEnd
     */
    public java.lang.Long getTimeEnd() {
        return timeEnd;
    }


    /**
     * Sets the timeEnd value for this DMIData.
     * 
     * @param timeEnd
     */
    public void setTimeEnd(java.lang.Long timeEnd) {
        this.timeEnd = timeEnd;
    }


    /**
     * Gets the timeout value for this DMIData.
     * 
     * @return timeout
     */
    public boolean isTimeout() {
        return timeout;
    }


    /**
     * Sets the timeout value for this DMIData.
     * 
     * @param timeout
     */
    public void setTimeout(boolean timeout) {
        this.timeout = timeout;
    }


    /**
     * Gets the timeoutValue value for this DMIData.
     * 
     * @return timeoutValue
     */
    public long getTimeoutValue() {
        return timeoutValue;
    }


    /**
     * Sets the timeoutValue value for this DMIData.
     * 
     * @param timeoutValue
     */
    public void setTimeoutValue(long timeoutValue) {
        this.timeoutValue = timeoutValue;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof DMIData)) return false;
        DMIData other = (DMIData) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.columnHeader==null && other.getColumnHeader()==null) || 
             (this.columnHeader!=null &&
              java.util.Arrays.equals(this.columnHeader, other.getColumnHeader()))) &&
            ((this.dmiServiceError==null && other.getDmiServiceError()==null) || 
             (this.dmiServiceError!=null &&
              java.util.Arrays.equals(this.dmiServiceError, other.getDmiServiceError()))) &&
            ((this.formattedData==null && other.getFormattedData()==null) || 
             (this.formattedData!=null &&
              java.util.Arrays.equals(this.formattedData, other.getFormattedData()))) &&
            ((this.rawData==null && other.getRawData()==null) || 
             (this.rawData!=null &&
              java.util.Arrays.equals(this.rawData, other.getRawData()))) &&
            ((this.timeBegin==null && other.getTimeBegin()==null) || 
             (this.timeBegin!=null &&
              this.timeBegin.equals(other.getTimeBegin()))) &&
            ((this.timeEnd==null && other.getTimeEnd()==null) || 
             (this.timeEnd!=null &&
              this.timeEnd.equals(other.getTimeEnd()))) &&
            this.timeout == other.isTimeout() &&
            this.timeoutValue == other.getTimeoutValue();
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
        if (getColumnHeader() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getColumnHeader());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getColumnHeader(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getDmiServiceError() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getDmiServiceError());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getDmiServiceError(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getFormattedData() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getFormattedData());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getFormattedData(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getRawData() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getRawData());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getRawData(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getTimeBegin() != null) {
            _hashCode += getTimeBegin().hashCode();
        }
        if (getTimeEnd() != null) {
            _hashCode += getTimeEnd().hashCode();
        }
        _hashCode += (isTimeout() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        _hashCode += new Long(getTimeoutValue()).hashCode();
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(DMIData.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://ws.delta.adlex", "DMIData"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("columnHeader");
        elemField.setXmlName(new javax.xml.namespace.QName("", "columnHeader"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/encoding/", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("dmiServiceError");
        elemField.setXmlName(new javax.xml.namespace.QName("", "dmiServiceError"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://ws.delta.adlex", "DMIServiceError"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("formattedData");
        elemField.setXmlName(new javax.xml.namespace.QName("", "formattedData"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/encoding/", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("rawData");
        elemField.setXmlName(new javax.xml.namespace.QName("", "rawData"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/encoding/", "double"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("timeBegin");
        elemField.setXmlName(new javax.xml.namespace.QName("", "timeBegin"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/encoding/", "long"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("timeEnd");
        elemField.setXmlName(new javax.xml.namespace.QName("", "timeEnd"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/encoding/", "long"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("timeout");
        elemField.setXmlName(new javax.xml.namespace.QName("", "timeout"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("timeoutValue");
        elemField.setXmlName(new javax.xml.namespace.QName("", "timeoutValue"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"));
        elemField.setNillable(false);
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
