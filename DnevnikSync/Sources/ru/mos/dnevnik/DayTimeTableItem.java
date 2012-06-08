/**
 * DayTimeTableItem.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package ru.mos.dnevnik;

public class DayTimeTableItem  implements java.io.Serializable {
    private org.apache.axis.types.UnsignedByte lessonNumber;

    private int startHours;

    private int startMinutes;

    private int finishHours;

    private int finishMinutes;

    public DayTimeTableItem() {
    }

    public DayTimeTableItem(
           org.apache.axis.types.UnsignedByte lessonNumber,
           int startHours,
           int startMinutes,
           int finishHours,
           int finishMinutes) {
           this.lessonNumber = lessonNumber;
           this.startHours = startHours;
           this.startMinutes = startMinutes;
           this.finishHours = finishHours;
           this.finishMinutes = finishMinutes;
    }


    /**
     * Gets the lessonNumber value for this DayTimeTableItem.
     * 
     * @return lessonNumber
     */
    public org.apache.axis.types.UnsignedByte getLessonNumber() {
        return lessonNumber;
    }


    /**
     * Sets the lessonNumber value for this DayTimeTableItem.
     * 
     * @param lessonNumber
     */
    public void setLessonNumber(org.apache.axis.types.UnsignedByte lessonNumber) {
        this.lessonNumber = lessonNumber;
    }


    /**
     * Gets the startHours value for this DayTimeTableItem.
     * 
     * @return startHours
     */
    public int getStartHours() {
        return startHours;
    }


    /**
     * Sets the startHours value for this DayTimeTableItem.
     * 
     * @param startHours
     */
    public void setStartHours(int startHours) {
        this.startHours = startHours;
    }


    /**
     * Gets the startMinutes value for this DayTimeTableItem.
     * 
     * @return startMinutes
     */
    public int getStartMinutes() {
        return startMinutes;
    }


    /**
     * Sets the startMinutes value for this DayTimeTableItem.
     * 
     * @param startMinutes
     */
    public void setStartMinutes(int startMinutes) {
        this.startMinutes = startMinutes;
    }


    /**
     * Gets the finishHours value for this DayTimeTableItem.
     * 
     * @return finishHours
     */
    public int getFinishHours() {
        return finishHours;
    }


    /**
     * Sets the finishHours value for this DayTimeTableItem.
     * 
     * @param finishHours
     */
    public void setFinishHours(int finishHours) {
        this.finishHours = finishHours;
    }


    /**
     * Gets the finishMinutes value for this DayTimeTableItem.
     * 
     * @return finishMinutes
     */
    public int getFinishMinutes() {
        return finishMinutes;
    }


    /**
     * Sets the finishMinutes value for this DayTimeTableItem.
     * 
     * @param finishMinutes
     */
    public void setFinishMinutes(int finishMinutes) {
        this.finishMinutes = finishMinutes;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof DayTimeTableItem)) return false;
        DayTimeTableItem other = (DayTimeTableItem) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.lessonNumber==null && other.getLessonNumber()==null) || 
             (this.lessonNumber!=null &&
              this.lessonNumber.equals(other.getLessonNumber()))) &&
            this.startHours == other.getStartHours() &&
            this.startMinutes == other.getStartMinutes() &&
            this.finishHours == other.getFinishHours() &&
            this.finishMinutes == other.getFinishMinutes();
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
        if (getLessonNumber() != null) {
            _hashCode += getLessonNumber().hashCode();
        }
        _hashCode += getStartHours();
        _hashCode += getStartMinutes();
        _hashCode += getFinishHours();
        _hashCode += getFinishMinutes();
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(DayTimeTableItem.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "DayTimeTableItem"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("lessonNumber");
        elemField.setXmlName(new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "LessonNumber"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "unsignedByte"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("startHours");
        elemField.setXmlName(new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "StartHours"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("startMinutes");
        elemField.setXmlName(new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "StartMinutes"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("finishHours");
        elemField.setXmlName(new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "FinishHours"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("finishMinutes");
        elemField.setXmlName(new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "FinishMinutes"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
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
