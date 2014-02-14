/**
 * DMIData3.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package adlex.delta.ws;

public class DMIData3  extends adlex.delta.ws.DMIData2  implements java.io.Serializable {
    private java.lang.String[] columnRendererIds;

    public DMIData3() {
    }

    public DMIData3(
           java.lang.String[] columnHeader,
           adlex.delta.ws.DMIServiceError[] dmiServiceError,
           java.lang.String[][] formattedData,
           java.lang.Double[][] rawData,
           java.lang.Long timeBegin,
           java.lang.Long timeEnd,
           boolean timeout,
           long timeoutValue,
           java.lang.String[] columnHeaderName,
           java.lang.String[] columnUnit,
           java.lang.String[] columnRendererIds) {
        super(
            columnHeader,
            dmiServiceError,
            formattedData,
            rawData,
            timeBegin,
            timeEnd,
            timeout,
            timeoutValue,
            columnHeaderName,
            columnUnit);
        this.columnRendererIds = columnRendererIds;
    }


    /**
     * Gets the columnRendererIds value for this DMIData3.
     * 
     * @return columnRendererIds
     */
    public java.lang.String[] getColumnRendererIds() {
        return columnRendererIds;
    }


    /**
     * Sets the columnRendererIds value for this DMIData3.
     * 
     * @param columnRendererIds
     */
    public void setColumnRendererIds(java.lang.String[] columnRendererIds) {
        this.columnRendererIds = columnRendererIds;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof DMIData3)) return false;
        DMIData3 other = (DMIData3) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = super.equals(obj) && 
            ((this.columnRendererIds==null && other.getColumnRendererIds()==null) || 
             (this.columnRendererIds!=null &&
              java.util.Arrays.equals(this.columnRendererIds, other.getColumnRendererIds())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = super.hashCode();
        if (getColumnRendererIds() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getColumnRendererIds());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getColumnRendererIds(), i);
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
        new org.apache.axis.description.TypeDesc(DMIData3.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://ws.delta.adlex", "DMIData3"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("columnRendererIds");
        elemField.setXmlName(new javax.xml.namespace.QName("", "columnRendererIds"));
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
