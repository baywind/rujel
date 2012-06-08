/**
 * ReportingPeriod.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package ru.mos.dnevnik;

public class ReportingPeriod  implements java.io.Serializable {
    private long ID;

    private org.apache.axis.types.UnsignedByte number;

    private java.util.Calendar dateStart;

    private java.util.Calendar dateFinish;

    public ReportingPeriod() {
    }

    public ReportingPeriod(
           long ID,
           org.apache.axis.types.UnsignedByte number,
           java.util.Calendar dateStart,
           java.util.Calendar dateFinish) {
           this.ID = ID;
           this.number = number;
           this.dateStart = dateStart;
           this.dateFinish = dateFinish;
    }


    /**
     * Gets the ID value for this ReportingPeriod.
     * 
     * @return ID
     */
    public long getID() {
        return ID;
    }


    /**
     * Sets the ID value for this ReportingPeriod.
     * 
     * @param ID
     */
    public void setID(long ID) {
        this.ID = ID;
    }


    /**
     * Gets the number value for this ReportingPeriod.
     * 
     * @return number
     */
    public org.apache.axis.types.UnsignedByte getNumber() {
        return number;
    }


    /**
     * Sets the number value for this ReportingPeriod.
     * 
     * @param number
     */
    public void setNumber(org.apache.axis.types.UnsignedByte number) {
        this.number = number;
    }


    /**
     * Gets the dateStart value for this ReportingPeriod.
     * 
     * @return dateStart
     */
    public java.util.Calendar getDateStart() {
        return dateStart;
    }


    /**
     * Sets the dateStart value for this ReportingPeriod.
     * 
     * @param dateStart
     */
    public void setDateStart(java.util.Calendar dateStart) {
        this.dateStart = dateStart;
    }


    /**
     * Gets the dateFinish value for this ReportingPeriod.
     * 
     * @return dateFinish
     */
    public java.util.Calendar getDateFinish() {
        return dateFinish;
    }


    /**
     * Sets the dateFinish value for this ReportingPeriod.
     * 
     * @param dateFinish
     */
    public void setDateFinish(java.util.Calendar dateFinish) {
        this.dateFinish = dateFinish;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof ReportingPeriod)) return false;
        ReportingPeriod other = (ReportingPeriod) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            this.ID == other.getID() &&
            ((this.number==null && other.getNumber()==null) || 
             (this.number!=null &&
              this.number.equals(other.getNumber()))) &&
            ((this.dateStart==null && other.getDateStart()==null) || 
             (this.dateStart!=null &&
              this.dateStart.equals(other.getDateStart()))) &&
            ((this.dateFinish==null && other.getDateFinish()==null) || 
             (this.dateFinish!=null &&
              this.dateFinish.equals(other.getDateFinish())));
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
        _hashCode += new Long(getID()).hashCode();
        if (getNumber() != null) {
            _hashCode += getNumber().hashCode();
        }
        if (getDateStart() != null) {
            _hashCode += getDateStart().hashCode();
        }
        if (getDateFinish() != null) {
            _hashCode += getDateFinish().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(ReportingPeriod.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "ReportingPeriod"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("ID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "ID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("number");
        elemField.setXmlName(new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "Number"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "unsignedByte"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("dateStart");
        elemField.setXmlName(new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "DateStart"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("dateFinish");
        elemField.setXmlName(new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "DateFinish"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"));
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
