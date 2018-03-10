package models;

public abstract class ApexType {
	
	/** getParserExpr returns an expression that can extract a value of this type 
		from the current position in the supplied json parser variable */
	public abstract String getParserExpr(String parserName);
	
	/** additionalMethods allows types to generate new methods into the top level class to aid in deserialization */
	public abstract String additionalMethods();
}