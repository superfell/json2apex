package models;

public class ApexPrimative extends ApexType {

	public static ApexPrimative STRING = new ApexPrimative("String");
	public static ApexPrimative INT    = new ApexPrimative("Integer");
	public static ApexPrimative DOUBLE = new ApexPrimative("Double");
	public static ApexPrimative OBJECT = new ApexPrimative("Object");
	public static ApexPrimative BOOLEAN= new ApexPrimative("Boolean");
	public static ApexPrimative LONG   = new ApexPrimative("Long");
	
	private ApexPrimative(String primativeType) {
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
		final int prime = 31;
		int result = 1;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ApexPrimative other = (ApexPrimative) obj;
		return type.equals(other.type);
	}
}
