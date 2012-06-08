/**
 * EduReportingPeriodType.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package ru.mos.dnevnik;

public class EduReportingPeriodType implements java.io.Serializable {
    private java.lang.String _value_;
    private static java.util.HashMap _table_ = new java.util.HashMap();

    // Constructor
    protected EduReportingPeriodType(java.lang.String value) {
        _value_ = value;
        _table_.put(_value_,this);
    }

    public static final java.lang.String _NotSet = "NotSet";
    public static final java.lang.String _Year = "Year";
    public static final java.lang.String _HalfYear = "HalfYear";
    public static final java.lang.String _Semester = "Semester";
    public static final java.lang.String _Trimester = "Trimester";
    public static final java.lang.String _Quarter = "Quarter";
    public static final java.lang.String _Module = "Module";
    public static final EduReportingPeriodType NotSet = new EduReportingPeriodType(_NotSet);
    public static final EduReportingPeriodType Year = new EduReportingPeriodType(_Year);
    public static final EduReportingPeriodType HalfYear = new EduReportingPeriodType(_HalfYear);
    public static final EduReportingPeriodType Semester = new EduReportingPeriodType(_Semester);
    public static final EduReportingPeriodType Trimester = new EduReportingPeriodType(_Trimester);
    public static final EduReportingPeriodType Quarter = new EduReportingPeriodType(_Quarter);
    public static final EduReportingPeriodType Module = new EduReportingPeriodType(_Module);
    public java.lang.String getValue() { return _value_;}
    public static EduReportingPeriodType fromValue(java.lang.String value)
          throws java.lang.IllegalArgumentException {
        EduReportingPeriodType enumeration = (EduReportingPeriodType)
            _table_.get(value);
        if (enumeration==null) throw new java.lang.IllegalArgumentException();
        return enumeration;
    }
    public static EduReportingPeriodType fromString(java.lang.String value)
          throws java.lang.IllegalArgumentException {
        return fromValue(value);
    }
    public boolean equals(java.lang.Object obj) {return (obj == this);}
    public int hashCode() { return toString().hashCode();}
    public java.lang.String toString() { return _value_;}
    public java.lang.Object readResolve() throws java.io.ObjectStreamException { return fromValue(_value_);}
    public static org.apache.axis.encoding.Serializer getSerializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new org.apache.axis.encoding.ser.EnumSerializer(
            _javaType, _xmlType);
    }
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new org.apache.axis.encoding.ser.EnumDeserializer(
            _javaType, _xmlType);
    }
    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(EduReportingPeriodType.class);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "EduReportingPeriodType"));
    }
    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

}
