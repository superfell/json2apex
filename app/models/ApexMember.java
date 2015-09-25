package models;

import java.util.Objects;

/**
	ApexMember track the name of a json field / apex class member
	in particular it handles the fact that the apex member name might
	be different to the json field name if the json field name clashes
	with a reserved work in apex.
*/
public class ApexMember {
	
	public ApexMember(String jsonFieldName, String apexMemberName) {
		this.jsonFieldName = jsonFieldName;
		this.apexMemberName = apexMemberName;
	}
	
	public final String jsonFieldName;
	public final String apexMemberName;
	
	/** @return an apex comment if the json & apex names are different */
	public String getMemberComment() {
		if (jsonFieldName.equals(apexMemberName)) {
			return "";
		}
		return "// in json: " + jsonFieldName;
	}
	
	@Override
	public String toString() {
		return jsonFieldName;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(jsonFieldName, apexMemberName);
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		if (o.getClass() != getClass()) return false;
		ApexMember rhs = (ApexMember)o;
		return rhs.jsonFieldName.equals(jsonFieldName) &&
			   rhs.apexMemberName.equals(apexMemberName);
	}
}