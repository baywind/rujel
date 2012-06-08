/**
 * EduLessonLogEntryStatus.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package ru.mos.dnevnik;

public class EduLessonLogEntryStatus implements java.io.Serializable {
    private java.lang.String _value_;
    private static java.util.HashMap _table_ = new java.util.HashMap();

    // Constructor
    protected EduLessonLogEntryStatus(java.lang.String value) {
        _value_ = value;
        _table_.put(_value_,this);
    }

    public static final java.lang.String _NotSet = "NotSet";
    public static final java.lang.String _Attend = "Attend";
    public static final java.lang.String _Absent = "Absent";
    public static final java.lang.String _Ill = "Ill";
    public static final java.lang.String _Late = "Late";
    public static final java.lang.String _Pass = "Pass";
    public static final EduLessonLogEntryStatus NotSet = new EduLessonLogEntryStatus(_NotSet);
    public static final EduLessonLogEntryStatus Attend = new EduLessonLogEntryStatus(_Attend);
    public static final EduLessonLogEntryStatus Absent = new EduLessonLogEntryStatus(_Absent);
    public static final EduLessonLogEntryStatus Ill = new EduLessonLogEntryStatus(_Ill);
    public static final EduLessonLogEntryStatus Late = new EduLessonLogEntryStatus(_Late);
    public static final EduLessonLogEntryStatus Pass = new EduLessonLogEntryStatus(_Pass);
    public java.lang.String getValue() { return _value_;}
    public static EduLessonLogEntryStatus fromValue(java.lang.String value)
          throws java.lang.IllegalArgumentException {
        EduLessonLogEntryStatus enumeration = (EduLessonLogEntryStatus)
            _table_.get(value);
        if (enumeration==null) throw new java.lang.IllegalArgumentException();
        return enumeration;
    }
    public static EduLessonLogEntryStatus fromString(java.lang.String value)
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
        new org.apache.axis.description.TypeDesc(EduLessonLogEntryStatus.class);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "EduLessonLogEntryStatus"));
    }
    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

}
