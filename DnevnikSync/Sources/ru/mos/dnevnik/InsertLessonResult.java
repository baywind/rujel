/**
 * InsertLessonResult.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package ru.mos.dnevnik;

public class InsertLessonResult  implements java.io.Serializable {
    private long lessonID;

    private long workID;

    public InsertLessonResult() {
    }

    public InsertLessonResult(
           long lessonID,
           long workID) {
           this.lessonID = lessonID;
           this.workID = workID;
    }


    /**
     * Gets the lessonID value for this InsertLessonResult.
     * 
     * @return lessonID
     */
    public long getLessonID() {
        return lessonID;
    }


    /**
     * Sets the lessonID value for this InsertLessonResult.
     * 
     * @param lessonID
     */
    public void setLessonID(long lessonID) {
        this.lessonID = lessonID;
    }


    /**
     * Gets the workID value for this InsertLessonResult.
     * 
     * @return workID
     */
    public long getWorkID() {
        return workID;
    }


    /**
     * Sets the workID value for this InsertLessonResult.
     * 
     * @param workID
     */
    public void setWorkID(long workID) {
        this.workID = workID;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof InsertLessonResult)) return false;
        InsertLessonResult other = (InsertLessonResult) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            this.lessonID == other.getLessonID() &&
            this.workID == other.getWorkID();
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
        _hashCode += new Long(getLessonID()).hashCode();
        _hashCode += new Long(getWorkID()).hashCode();
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(InsertLessonResult.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "InsertLessonResult"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("lessonID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "LessonID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("workID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "WorkID"));
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
