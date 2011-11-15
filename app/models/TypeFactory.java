package models;

import java.util.*;

public class TypeFactory {
	
	private static final Set<String> reserved;
	
	static {
		// reserved words taken from docs
		// at http://www.salesforce.com/us/developer/docs/apexcode/index_Left.htm#StartTopic=Content/apex_reserved_words.htm
		reserved = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[] {
			"abstract", "activate", "and", "any", "array", "as", "asc", "autonomous",
			"begin", "bigdecimal", "blob", "break","bulk", "by", "byte",
			"case","cast","catch","char","class","collect","commit","const","continue","convertcurrency",
			"decimal","default","delete","desc","do",
			"else","end","enum","exit","export","extends","false","final","finally","float","for","from","future",
			"global","goto","group",
			"having","hint","if","implements","import","inner","insert","instanceof","interface","into","int",
			"join",
			"last_90_days","last_month","last_n_days","last_week","like","limit","list","long","loop",
			"map","merge",
			"new","next_90_days","next_month","next_n_days","next_week","not","null","nulls","number",
			"object","of","on","or","outer","override",
			"package","parallel","pragma","private","protected","public",
			"retrieve","return","returning","rollback",
			"savepoint","search","select","set","short","sort","stat","super","switch","synchronized","system",
			"testmethod","then","this","this_month","this_week","throw","today","tolabel","tomorrow","transaction",
			"trigger","true","try","type",
			"undelete","update","upsert","using",
			"virtual",
			"webservice","when","where","while",
			"yesterday"
		})));
	}

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
		while (classes.containsKey(proposed) || reserved.contains(proposed.toLowerCase())) {
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