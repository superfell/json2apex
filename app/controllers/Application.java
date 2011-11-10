package controllers;

import play.*;
import play.mvc.*;
import play.data.validation.*;
import org.codehaus.jackson.map.*;
import java.io.IOException;

import java.util.*;
import models.*;

public class Application extends Controller {

    public static void index() {
        render();
    }

 	public static void makeApex(@Required String json, @Required String className) {
	 	if (Validation.hasErrors()) {
	        flash.error("Oops, please enter your json and className!");
	        index();
	    }
		try {
			ObjectMapper m = new ObjectMapper();
			Object o = m.readValue(json, Object.class);
			TypeFactory factory = new TypeFactory();
			ApexType root = factory.typeOfObject("Root", o);
			StringBuilder def = new StringBuilder();
			if (factory.getClasses().size() > 0) {
				def.append("public class ").append(className).append(" {\n\n");
				for (ApexClass c : factory.getClasses()) {
					c.writeClassDefinition(def);
				}
				def.append("\n");
				def.append("\tpublic static ").append(root.toString());
				def.append(" parseJson(String json) {\n\t\treturn (");
				def.append(root.toString()).append(")System.JSON.deserialize(json, ").append(root.toString()).append(".class);\n\t}\n");
				def.append("\n}\n");
			}
			renderText(def.toString());
			
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
    }
}