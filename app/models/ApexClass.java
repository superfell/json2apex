package models;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public class ApexClass extends ApexType {

	ApexClass(String className, Map<ApexMember, ApexType> members) {
		if (className == null) throw new NullPointerException();
		if (members == null) throw new NullPointerException();
		this.className = className;
		this.members = members;
	}
	
	private final String className;
	private final Map<ApexMember, ApexType> members;
	
	public Map<ApexMember, ApexType> getMembers() {
		return members;
	}
	
	@Override
	public String getParserExpr(String parserName) {
		return String.format("new %s(%s)", className, parserName);
	}
	
	@Override
	public String additionalMethods() {
		return "";
	}
	
	public boolean shouldGenerateExplictParse() {
		for (ApexMember m : members.keySet()) {
			if (m.shouldGenerateExplictParse()) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public String toString() {
		return className;
	}

	@Override
	public int hashCode() {
		return Objects.hash(className, members);
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
	boolean membersEqual(Map<ApexMember, ApexType> other) {
		return members.equals(other);
	}
	
	void mergeFields(ApexClass other) {
		for (ApexMember key : other.getMembers().keySet() ) {
			if (members.get(key) == null) {
				members.put(key, other.getMembers().get(key));
			}
		}
	}
}
