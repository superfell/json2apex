package models;

public class ApexPrimitive extends ApexType {

	public static ApexPrimitive STRING = new ApexPrimitive("String");
	public static ApexPrimitive INT    = new ApexPrimitive("Integer");
	public static ApexPrimitive DOUBLE = new ApexPrimitive("Double");
	public static ApexPrimitive BOOLEAN= new ApexPrimitive("Boolean");
	public static ApexPrimitive LONG   = new ApexPrimitive("Long");
	public static ApexPrimitive OBJECT = new ApexPrimitive("Object");
	
	private ApexPrimitive(String primativeType) {
		if (primativeType == null) throw new NullPointerException();
		this.type = primativeType;
	}
	
	private final String type;

	@Override
	public String toString() {
		return type;
	}
	
	@Override
	public int hashCode() {
		return type.hashCode() + 1;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ApexPrimitive other = (ApexPrimitive) obj;
		return type.equals(other.type);
	}
}
