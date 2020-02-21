package models;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
	
	Set<String> mergeFields(ApexClass other) {
		Set<String> classesToRemove = new HashSet<>();

		for (ApexMember key : other.getMembers().keySet() ) {
			// If our member is an array and the other member is also an array check the item types of
			// each array and if they're both ApexClass' then merge them.
			if (members.get(key) instanceof ApexList && other.getMembers().get(key) instanceof ApexList) {
				ApexList ourList = (ApexList) members.get(key);
				ApexList otherList = (ApexList) other.getMembers().get(key);

				if (ourList.itemType instanceof ApexClass && otherList.itemType instanceof ApexClass) {
					ApexClass itemType = (ApexClass) ourList.itemType;
					ApexClass otherType = (ApexClass) otherList.itemType;

					classesToRemove.addAll(itemType.mergeFields(otherType));
				}
			}

			// Cover case where two members of the same name exist, and both are classes, and so we want to
			// recursively merge the members of those classes.
			if (members.get(key) instanceof ApexClass && other.getMembers().get(key) instanceof ApexClass) {
				classesToRemove.addAll(((ApexClass) members.get(key)).mergeFields((ApexClass) other.getMembers().get(key)));
			}

			// Merge a member if it's null, or if the existing member is an Object, because
			// that Object may have been originally just from a null value, and is not necessarily
			// determinant of it's final type.
			if (members.get(key) == null || members.get(key) == ApexPrimitive.OBJECT) {
				members.put(key, other.getMembers().get(key));
				classesToRemove.add(other.toString());
			}
		}

		return classesToRemove;
	}
}
