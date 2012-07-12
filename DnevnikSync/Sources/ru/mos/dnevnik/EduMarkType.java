/**
 * EduMarkType.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package ru.mos.dnevnik;

public class EduMarkType implements java.io.Serializable {
    private java.lang.String _value_;
    private static java.util.HashMap _table_ = new java.util.HashMap();

    // Constructor
    protected EduMarkType(java.lang.String value) {
        _value_ = value;
        _table_.put(_value_,this);
    }

    public static final java.lang.String _NotSet = "NotSet";
    public static final java.lang.String _Test = "Test";
    public static final java.lang.String _Mark5 = "Mark5";
    public static final java.lang.String _Mark7 = "Mark7";
    public static final java.lang.String _Mark100 = "Mark100";
    public static final java.lang.String _MarkNA = "MarkNA";
    public static final java.lang.String _MarkDismiss = "MarkDismiss";
    public static final java.lang.String _MarkAbcdf = "MarkAbcdf";
    public static final java.lang.String _Mark12 = "Mark12";
    public static final java.lang.String _MarkOral6Band = "MarkOral6Band";
    public static final java.lang.String _Mark6 = "Mark6";
    public static final java.lang.String _Mark10 = "Mark10";
    public static final EduMarkType NotSet = new EduMarkType(_NotSet);
    public static final EduMarkType Test = new EduMarkType(_Test);
    public static final EduMarkType Mark5 = new EduMarkType(_Mark5);
    public static final EduMarkType Mark7 = new EduMarkType(_Mark7);
    public static final EduMarkType Mark100 = new EduMarkType(_Mark100);
    public static final EduMarkType MarkNA = new EduMarkType(_MarkNA);
    public static final EduMarkType MarkDismiss = new EduMarkType(_MarkDismiss);
    public static final EduMarkType MarkAbcdf = new EduMarkType(_MarkAbcdf);
    public static final EduMarkType Mark12 = new EduMarkType(_Mark12);
    public static final EduMarkType MarkOral6Band = new EduMarkType(_MarkOral6Band);
    public static final EduMarkType Mark6 = new EduMarkType(_Mark6);
    public static final EduMarkType Mark10 = new EduMarkType(_Mark10);
    public java.lang.String getValue() { return _value_;}
    public static EduMarkType fromValue(java.lang.String value)
          throws java.lang.IllegalArgumentException {
        EduMarkType enumeration = (EduMarkType)
            _table_.get(value);
        if (enumeration==null) throw new java.lang.IllegalArgumentException();
        return enumeration;
    }
    public static EduMarkType fromString(java.lang.String value)
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
        new org.apache.axis.description.TypeDesc(EduMarkType.class);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "EduMarkType"));
    }
    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

}
