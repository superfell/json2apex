package models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TypeFactory {
	
	public TypeFactory() {
		this.classes = new ArrayList<ApexClass>();
	}
	
	private final List<ApexClass> classes;
	
	public List<ApexClass> getClasses() {
		return classes;
	}
	
	public ApexType typeOfObject(String propertyName, Object o) {
		if (o instanceof List) {
			return new ApexList(typeOfCollection(propertyName, (List)o));
		} else if (o instanceof Map) {
			return typeOfMap(propertyName, (Map)o);
		} else if (o instanceof String) {
			return ApexPrimative.STRING;
		} else if (o instanceof Integer) {
			return ApexPrimative.INT;
		} else if (o instanceof Double) {
			return ApexPrimative.DOUBLE;
		}
		throw new RuntimeException("Unexpected type " + o.getClass() + " in TypeFactory.typeOfObject()");
	}
	
	/** @return a concrete type if all the list items are of the same type, Object, otherwise */
	ApexType typeOfCollection(String propertyName, Collection<?> col) {
		if (col == null || col.size() == 0) return ApexPrimative.OBJECT;
		ApexType itemType = null;
		for (Object o : col) {
			ApexType thisItemType = typeOfObject(propertyName, o);
			if (itemType == null) {
				itemType = thisItemType;
			} else if (!itemType.equals(thisItemType)) {
				return ApexPrimative.OBJECT;
			}
		}
		return itemType;
	}
	
	/** @return an ApexClass for this map */
	ApexType typeOfMap(String propertyName, Map o) {
		Map<String, ApexType> members = makeMembers(o);
		// see if any existing classes have the same member set
		for (ApexClass cls : classes) {
			if (cls.membersEqual(members))
				return cls; 
		}
		ApexClass newClass = new ApexClass(propertyName, members);
		classes.add(newClass);
		return newClass;
	}
	
	private Map<String, ApexType> makeMembers(Map<String, Object> o) {
		Map<String, ApexType> members = new LinkedHashMap<String, ApexType>();
		for (Map.Entry<String, Object> e : o.entrySet()) {
			ApexType memberType = typeOfObject(e.getKey(), e.getValue());
			members.put(e.getKey(), memberType);
		}
		return members;
	}
}