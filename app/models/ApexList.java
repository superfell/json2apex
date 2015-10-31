package models;

class ApexList extends ApexType {
	
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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + itemType.hashCode();
		return result;
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