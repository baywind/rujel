/**
 * DayTimeTable.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package ru.mos.dnevnik;

public class DayTimeTable  implements java.io.Serializable {
    private ru.mos.dnevnik.DayTimeTableItem[] itemCollection;

    public DayTimeTable() {
    }

    public DayTimeTable(
           ru.mos.dnevnik.DayTimeTableItem[] itemCollection) {
           this.itemCollection = itemCollection;
    }


    /**
     * Gets the itemCollection value for this DayTimeTable.
     * 
     * @return itemCollection
     */
    public ru.mos.dnevnik.DayTimeTableItem[] getItemCollection() {
        return itemCollection;
    }


    /**
     * Sets the itemCollection value for this DayTimeTable.
     * 
     * @param itemCollection
     */
    public void setItemCollection(ru.mos.dnevnik.DayTimeTableItem[] itemCollection) {
        this.itemCollection = itemCollection;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof DayTimeTable)) return false;
        DayTimeTable other = (DayTimeTable) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.itemCollection==null && other.getItemCollection()==null) || 
             (this.itemCollection!=null &&
              java.util.Arrays.equals(this.itemCollection, other.getItemCollection())));
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
        if (getItemCollection() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getItemCollection());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getItemCollection(), i);
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
        new org.apache.axis.description.TypeDesc(DayTimeTable.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "DayTimeTable"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("itemCollection");
        elemField.setXmlName(new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "ItemCollection"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "DayTimeTableItem"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        elemField.setItemQName(new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "DayTimeTableItem"));
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
