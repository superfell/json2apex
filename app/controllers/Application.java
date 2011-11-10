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

 	public static void makeApex(@Required String json) {
	 	if (Validation.hasErrors()) {
	        flash.error("Oops, please enter your json!");
	        index();
	    }
		try {
			ObjectMapper m = new ObjectMapper();
			Object o = m.readValue(json, Object.class);
			TypeFactory factory = new TypeFactory();
			ApexType root = factory.typeOfObject("Root", o);
			StringBuilder def = new StringBuilder();
			if (factory.getClasses().size() > 0) {
				def.append("public class ruby99 {\n\n");
				for (ApexClass c : factory.getClasses()) {
					c.writeClassDefinition(def);
				}
				def.append("\n}\n");
			}
			renderText(def.toString());
			
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
    }
}