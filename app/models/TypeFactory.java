package models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TypeFactory {
	
	public TypeFactory() {
		this.classes = new HashMap<String, ApexClass>();
	}
	
	private final HashMap<String, ApexClass> classes;
	
	public Collection<ApexClass> getClasses() {
		return classes.values();
	}
	
	/** @return an ApexType that is the mapping of the json object instance 'o' */
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
		} else if (o instanceof Boolean) {
			return ApexPrimative.BOOLEAN;
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
		for (ApexClass cls : classes.values()) {
			if (cls.membersEqual(members))
				return cls; 
		}
		String newClassName = getClassName(propertyName);
		ApexClass newClass = new ApexClass(newClassName, members);
		classes.put(newClassName, newClass);
		return newClass;
	}
	
	/** converts a Map of json types into a map of ApexType's */
	private Map<String, ApexType> makeMembers(Map<String, Object> o) {
		Map<String, ApexType> members = new LinkedHashMap<String, ApexType>();
		for (Map.Entry<String, Object> e : o.entrySet()) {
			ApexType memberType = typeOfObject(e.getKey(), e.getValue());
			members.put(e.getKey(), memberType);
		}
		return members;
	}
	
	private String getClassName(String proposed) {
		boolean first = true;
		char letter = 'Z';
		while (classes.containsKey(proposed)) {
			if (first) proposed = proposed + "_Z";
			if (!proposed.endsWith("A"))
				proposed = proposed.substring(0, proposed.length()-1);
			proposed = proposed + letter;
			letter = (letter == 'A') ? 'Z' : (char)(letter-1);
			first = false;
		}
		return proposed;
	}
}