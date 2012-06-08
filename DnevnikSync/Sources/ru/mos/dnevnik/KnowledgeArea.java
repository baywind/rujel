/**
 * KnowledgeArea.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package ru.mos.dnevnik;

public class KnowledgeArea implements java.io.Serializable {
    private java.lang.String _value_;
    private static java.util.HashMap _table_ = new java.util.HashMap();

    // Constructor
    protected KnowledgeArea(java.lang.String value) {
        _value_ = value;
        _table_.put(_value_,this);
    }

    public static final java.lang.String _Philology = "Philology";
    public static final java.lang.String _Mathematics = "Mathematics";
    public static final java.lang.String _SocialStudies = "SocialStudies";
    public static final java.lang.String _NaturalScience = "NaturalScience";
    public static final java.lang.String _Art = "Art";
    public static final java.lang.String _PhysicalCulture = "PhysicalCulture";
    public static final java.lang.String _Technology = "Technology";
    public static final java.lang.String _Common = "Common";
    public static final KnowledgeArea Philology = new KnowledgeArea(_Philology);
    public static final KnowledgeArea Mathematics = new KnowledgeArea(_Mathematics);
    public static final KnowledgeArea SocialStudies = new KnowledgeArea(_SocialStudies);
    public static final KnowledgeArea NaturalScience = new KnowledgeArea(_NaturalScience);
    public static final KnowledgeArea Art = new KnowledgeArea(_Art);
    public static final KnowledgeArea PhysicalCulture = new KnowledgeArea(_PhysicalCulture);
    public static final KnowledgeArea Technology = new KnowledgeArea(_Technology);
    public static final KnowledgeArea Common = new KnowledgeArea(_Common);
    public java.lang.String getValue() { return _value_;}
    public static KnowledgeArea fromValue(java.lang.String value)
          throws java.lang.IllegalArgumentException {
        KnowledgeArea enumeration = (KnowledgeArea)
            _table_.get(value);
        if (enumeration==null) throw new java.lang.IllegalArgumentException();
        return enumeration;
    }
    public static KnowledgeArea fromString(java.lang.String value)
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
        new org.apache.axis.description.TypeDesc(KnowledgeArea.class);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://dnevnik.mos.ru/", "KnowledgeArea"));
    }
    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

}
