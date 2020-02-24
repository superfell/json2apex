package models;

import java.util.Objects;

public class ApexPrimitive extends ApexType {

	public static ApexPrimitive STRING = new ApexPrimitive("String",  "{p}.getText()");
	public static ApexPrimitive INT    = new ApexPrimitive("Integer", "{p}.getIntegerValue()");
	public static ApexPrimitive DOUBLE = new ApexPrimitive("Double",  "{p}.getDoubleValue()");
	public static ApexPrimitive BOOLEAN= new ApexPrimitive("Boolean", "{p}.getBooleanValue()");
	public static ApexPrimitive LONG   = new ApexPrimitive("Long",    "{p}.getLongValue()");
	
	public static ApexPrimitive OBJECT = new ApexPrimitive("Object",  "{p}.readValueAs(Object.class)");
	
	private ApexPrimitive(String primativeType, String parserMethod) {
		if (primativeType == null) throw new NullPointerException();
		this.type = primativeType;
		this.parserMethod = parserMethod;
	}
	
	private final String type;
	private final String parserMethod;

	@Override
	public String toString() {
		return type;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(type, parserMethod);
	}

	@Override
	public String getParserExpr(String parserName) {
		return parserMethod.replace("{p}", parserName);
	}
	
	@Override
	public String additionalMethods() {
		return "";
	}
	
	boolean canBePromotedTo(ApexPrimitive other) {
		return (this.equals(INT)) && (other.equals(LONG));
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (this.type.equals("Object")) return true;
		if (getClass() != obj.getClass()) return false;

		ApexPrimitive other = (ApexPrimitive) obj;
		return other.type.equals("Object") || type.equals(other.type);
	}
}
