/**
 * ReportingPeriodGroup.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package ru.mos.dnevnik;

public class ReportingPeriodGroup  implements java.io.Serializable {
    private long ID;

    private java.lang.String name;

    private ru.mos.dnevnik.EduReportingPeriodType type;

    private ru.mos.dnevnik.ReportingPeriod[] reportingPeriods;

    public ReportingPeriodGroup() {
    }

    public ReportingPeriodGroup(
           long ID,
           java.lang.String name,
           ru.mos.dnevnik.EduReportingPeriodType type,
           ru.mos.dnevnik.ReportingPeriod[] reportingPeriods) {
           this.ID = ID;
           this.name = name;
           this.type = type;
           this.reportingPeriods = reportingPeriods;
    }


    /**
     * Gets the ID value for this ReportingPeriodGroup.
     * 
     * @return ID
     */
    public long getID() {
        return ID;
    }


    /**
     * Sets the ID value for this ReportingPeriodGroup.
     * 
     * @param ID
     */
    public void setID(long ID) {
        this.ID = ID;
    }


    /**
     * Gets the name value for this ReportingPeriodGroup.
     * 
     * @return name
     */
    public java.lang.String getName() {
        return name;
    }


    /**
     * Sets the name value for this ReportingPeriodGroup.
     * 
     * @param name
     */
    public void setName(java.lang.String name) {
        this.name = name;
    }


    /**
     * Gets the type value for this ReportingPeriodGroup.
     * 
     * @return type
     */
    public ru.mos.dnevnik.EduReportingPeriodType getType() {
        return type;
    }


    /**
     * Sets the type value for this ReportingPeriodGroup.
     * 
     * @param type
     */
    public void setType(ru.mos.dnevnik.EduReportingPeriodType type) {
        this.type = type;
    }


    /**
     * Gets the reportingPeriods value for this ReportingPeriodGroup.
     * 
     * @return reportingPeriods
     */
    public ru.mos.dnevnik.ReportingPeriod[] getReportingPeriods() {
        return reportingPeriods;
    }


    /**
     * Sets the reportingPeriods value for this ReportingPeriodGroup.
     * 
     * @param reportingPeriods
     */
    public void setReportingPeriods(ru.mos.dnevnik.ReportingPeriod[] reportingPeriods) {
        this.reportingPeriods = reportingPeriods;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof ReportingPeriodGroup)) return false;
        ReportingPeriodGroup other = (ReportingPeriodGroup) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            this.ID == other.getID() &&
            ((this.name==null && other.getName()==null) || 
             (this.name!=null &&
              this.name.equals(other.getName()))) &&
            ((this.type==null && other.getType()==null) || 
             (this.type!=null &&
              this.type.equals(other.getType()))) &&
            ((this.reportingPeriods==null && other.getReportingPeriods()==null) || 
             (this.reportingPeriods!=null &&
              java.util.Arrays.equals(this.reportingPeriods, other.getReportingPeriods())));
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
        if (getName() != null) {
            _hashCode += getName().hashCode();
        }
        if (getType() != null) {
            _hashCode += getType().hashCode();
        }
        if (getReportingPeriods() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getReportingPeriods());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getReportingPeriods(), i);
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
        new org.apache.axis.description.TypeDesc(ReportingPeriodGroup.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "ReportingPeriodGroup"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("ID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "ID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("name");
        elemField.setXmlName(new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "Name"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("type");
        elemField.setXmlName(new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "Type"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "EduReportingPeriodType"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("reportingPeriods");
        elemField.setXmlName(new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "ReportingPeriods"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "ReportingPeriod"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        elemField.setItemQName(new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "ReportingPeriod"));
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
