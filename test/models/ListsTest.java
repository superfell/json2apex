package models;

import org.junit.*;
import java.util.*;
import play.test.*;
import models.*;
import org.junit.*;

public class ListsTest extends UnitTest {

	@Test
	public void testArrayPrimitives() {
 		List<Integer> l = new ArrayList<Integer>();
		l.add(1);
		l.add(2);
		TypeFactory factory = new TypeFactory();
 		ApexType root = factory.typeOfObject("Root", l);
		if (!(root instanceof ApexList)) {
			fail("Created ApexType should be a list, but is " + root);
		}
		ApexList al = (ApexList)root;
		assertEquals("ApexList ItemType is wrong", ApexPrimitive.INT, al.itemType);
	}
	
	@Test
	public void testArrayPromotionIntToLong() {
		List<Object> l = new ArrayList<Object>();
		l.add(1);
		l.add(3L);
		TypeFactory factory = new TypeFactory();
 		ApexType root = factory.typeOfObject("Root", l);
		if (!(root instanceof ApexList)) {
			fail("Created ApexType should be a list, but is " + root);
		}
		ApexList al = (ApexList)root;
		assertEquals("ApexList ItemType is wrong", ApexPrimitive.LONG, al.itemType);
	}

	@Test
	public void testArrayPromotionLongToInt() {
		List<Object> l = new ArrayList<Object>();
		l.add(3L);
		l.add(1);
		TypeFactory factory = new TypeFactory();
 		ApexType root = factory.typeOfObject("Root", l);
		if (!(root instanceof ApexList)) {
			fail("Created ApexType should be a list, but is " + root);
		}
		ApexList al = (ApexList)root;
		assertEquals("ApexList ItemType is wrong", ApexPrimitive.LONG, al.itemType);
	}
}
