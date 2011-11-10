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
			List<ApexClass> classes = factory.getClasses();
			request.format = "txt";
			render(className, root, classes);
			
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
    }
}