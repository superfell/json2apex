package models;

import java.io.IOException;
import java.util.Map;

public class ApexClass extends ApexType {

	ApexClass(String className, Map<String, ApexType> members) {
		if (className == null) throw new NullPointerException();
		if (members == null) throw new NullPointerException();
		this.className = className.length() > 1 ? className.substring(0, 1).toUpperCase() + className.substring(1) : className;
		this.members = members;
	}
	
	private final String className;
	private final Map<String, ApexType> members;
	
	public Map<String, ApexType> getMembers() {
		return members;
	}
	
	@Override
	public String toString() {
		return className;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + className.hashCode();
		result = prime * result + members.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ApexClass other = (ApexClass) obj;
		return className.equals(other.className);
	}
	
	/** @return true if this map of members equals our map of members */
	boolean membersEqual(Map<String, ApexType> other) {
		return members.equals(other);
	}
}
