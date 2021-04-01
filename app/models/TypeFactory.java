package models;

import java.util.*;
import java.util.function.Predicate;

public class TypeFactory {
	
	private static final Set<String> reserved;
	
	static {
		// reserved words taken from docs
		// at http://www.salesforce.com/us/developer/docs/apexcode/index_Left.htm#StartTopic=Content/apex_reserved_words.htm
		reserved = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[] {
			"abstract", "activate", "and", "any", "array", "as", "asc", "autonomous", 
			"begin", "bigdecimal", "blob", "boolean", "break", "bulk", "by", "byte", 
			"case", "cast", "catch", "char", "class", "collect", "commit", "const", "continue", "currency", 
			"date", "datetime", "decimal", "default", "delete", "desc", "do", "double", 
			"else", "end", "enum", "exception", "exit", "export", "extends", 
			"false", "final", "finally", "float", "for", "from", 
			"global", "goto", "group", 
			"having", "hint", 
			"if", "implements", "import", "in", "inner", "insert", "instanceof", "int", "integer", "interface", "into", 
			"join", 
			"like", "limit", "list", "long", "loop", 
			"map", "merge", 
			"new", "not", "null", "nulls", "number", 
			"object", "of", "on", "or", "outer", "override", 
			"package", "parallel", "pragma", "private", "protected", "public", 
			"retrieve", "return", "rollback", 
			"select", "set", "short", "sObject", "sort", "static", "string", "super", "switch", "synchronized", "system", 
			"testmethod", "then", "this", "throw", "time", "transaction", "trigger", "true", "try", 
			"undelete", "update", "upsert", "using", 
			"virtual", "void", 
			"webservice", "when", "where", "while"
		})));
	}

	public TypeFactory() {
		this.classes = new HashMap<String, ApexClass>();
		this.allTypes = new HashSet<ApexType>();
	}

	private final HashMap<String, ApexClass> classes;
	private final HashSet<ApexType> allTypes;
	
	public Collection<ApexClass> getClasses() {
		return classes.values();
	}
	
	public Collection<ApexType> allTypes() {
		return allTypes;
	}
	
	public boolean shouldGenerateExplictParse() {
		for (ApexClass c : classes.values()) {
			if (c.shouldGenerateExplictParse()) {
				return true;
			}
		}
		return false;
	}
	
	/** @return an ApexType that is the mapping of the json object instance 'o' */
	public ApexType typeOfObject(String propertyName, Object o) {
		ApexType t = typeOfObjectImpl(propertyName, o);
		allTypes.add(t);
		return t;
	}
	
	/** @return an ApexType that is the mapping of the json object instance 'o' */
	private ApexType typeOfObjectImpl(String propertyName, Object o) {
		if (o == null)
			return ApexPrimitive.OBJECT;
		if (o instanceof List) 
			return new ApexList(typeOfCollection(propertyName, (List)o));
		if (o instanceof Map) 
			return typeOfMap(propertyName, (Map)o);
		if (o instanceof String) 
			return ApexPrimitive.STRING;
		if (o instanceof Integer) 
			return ApexPrimitive.INT;
		if (o instanceof Long)
			return ApexPrimitive.LONG;
		if (o instanceof Double) 
			return ApexPrimitive.DOUBLE;
		if (o instanceof Boolean) 
			return ApexPrimitive.BOOLEAN;
		
		throw new RuntimeException("Unexpected type " + o.getClass() + " in TypeFactory.typeOfObject()");
	}
	
	/** @return a concrete type if all the list items are of the same type, Object, otherwise */
	ApexType typeOfCollection(String propertyName, Collection<?> col) {
		if (col == null || col.size() == 0) { 
			return typeOfObject(propertyName, Collections.EMPTY_MAP);
		}
		ApexType itemType = null;
		for (Object o : col) {
			ApexType thisItemType = typeOfObject(propertyName, o);
			if (itemType == null) {
				itemType = thisItemType;
			} else if (!itemType.equals(thisItemType)) {
				if (itemType instanceof ApexClass && thisItemType instanceof ApexClass) {
					ApexClass apexClass = (ApexClass)itemType;
					ApexClass thisApexClass = (ApexClass)thisItemType;
					
  					apexClass.mergeFields(thisApexClass);

					classes.remove(thisApexClass.toString());
				} else if (itemType instanceof ApexPrimitive && thisItemType instanceof ApexPrimitive) {
					ApexPrimitive a = (ApexPrimitive)itemType;
					ApexPrimitive b = (ApexPrimitive)thisItemType;
					if (a.canBePromotedTo(b)) {
						itemType = b;
					} else if (b.canBePromotedTo(a)) {
						continue;
					}
				} else {
					throw new RuntimeException("Can't add an " + o.getClass() + " to a collection of " + itemType.getClass());
  				}
			}
		}
		return itemType;
	}
	
	/** @return an ApexClass for this map */
	ApexType typeOfMap(String propertyName, Map o) {
		Map<ApexMember, ApexType> members = makeMembers(o);
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
	private Map<ApexMember, ApexType> makeMembers(Map<String, Object> o) {
		Map<ApexMember, ApexType> members = new LinkedHashMap<>();
		Set<String> fieldNames = new HashSet<>();
		for (String m : o.keySet()) {
			fieldNames.add(m.toLowerCase());
		}
		for (Map.Entry<String, Object> e : o.entrySet()) {
			ApexType memberType = typeOfObject(e.getKey(), e.getValue());
			ApexMember memberName = new ApexMember(e.getKey(), getApexMemberName(e.getKey(), fieldNames));
			members.put(memberName, memberType);
		}
		return members;
	}
	
	private String getClassName(String proposed) {
		proposed = proposed.replace(".", "_");
		proposed = proposed.replace("-", "_");

		if (proposed.length() > 1 && proposed.charAt(0) == '_') {
			proposed = proposed.substring(1);
		}

		proposed = proposed.length() > 1 ? proposed.substring(0, 1).toUpperCase() + proposed.substring(1) : proposed;
		return getSafeName(proposed, (p) -> !(classes.containsKey(p) || reserved.contains(p.toLowerCase())));
	}

	// getSafeName will try alternatives of proposed until the checker says its okay
	private String getSafeName(String proposed, Predicate<String> checker) {
		if (checker.test(proposed)) {
			return proposed;
		}
		char letter = 'Z';
		proposed = proposed + "_" + letter;
		while (!checker.test(proposed)) {
			if (!proposed.endsWith("A"))
				proposed = proposed.substring(0, proposed.length()-1);
			proposed = proposed + letter;
			letter = (letter == 'A') ? 'Z' : (char)(letter-1);
		}
		return proposed;
	}
	
	// returns a version of name that's safe to use as an apex class member name
	String getApexMemberName(String name, Set<String> otherMemberNames) {
		name = name.replace(".", "_");
		name = name.replace("-", "_");

		if (name.startsWith("_")) {
			return getSafeName("x" + name, (p) -> !(reserved.contains(p.toLowerCase()) || otherMemberNames.contains(p.toLowerCase())));
		}

		if (!reserved.contains(name.toLowerCase())) {
			return name;
		}
		return getSafeName(name, (p) -> !(reserved.contains(p.toLowerCase()) || otherMemberNames.contains(p.toLowerCase())));
	}
}