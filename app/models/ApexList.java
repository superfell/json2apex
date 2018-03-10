package models;

import java.util.Objects;

public class ApexList extends ApexType {
	
	ApexList(ApexType itemType) {
		if (itemType == null) throw new NullPointerException();
		this.itemType = itemType;
	}
	
	final ApexType itemType;

	@Override
	public String toString() {
		return "List<" + itemType.toString() + ">";
	}
	
	public String itemType() {
		return itemType.toString();
	}
	
	@Override
	public String getParserExpr(String parserName) {
		return String.format("arrayOf%s(%s)", itemType.toString(), parserName);
	}
	
	@Override
	public String additionalMethods() {
		String myType = this.toString();
		return String.format("    private static %s arrayOf%s(System.JSONParser p) {\n" + 
			"        %s res = new %s();\n" + 
			"        while (p.nextToken() != System.JSONToken.END_ARRAY) {\n" + 
			"            res.add(%s);\n" +
			"        }\n" + 
			"        return res;\n" +
			"    }\n\n",
				myType, this.itemType.toString(), myType, myType, itemType.getParserExpr("p"));
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(itemType);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ApexList other = (ApexList) obj;
		return itemType.equals(other.itemType);
	}
}