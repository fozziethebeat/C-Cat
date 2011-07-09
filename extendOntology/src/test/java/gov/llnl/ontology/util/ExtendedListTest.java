

package gov.llnl.ontology.util;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.*;


/**
 * @author Keith Stevens
 */
public class ExtendedListTest {

    private static final List<String> array1 = 
        Arrays.asList(new String[] {"one", "two", "three"});

    private static final List<String> array2 = 
        Arrays.asList(new String[] {"one", "two", "three"});

    @Test public void testAdd() {
        List<String> extendedList = new ExtendedList<String>(array1);
        for (String s : array2)
            assertTrue(extendedList.add(s));
        assertEquals(3, array1.size());
    }

    @Test public void testSize() {
        List<String> extendedList = new ExtendedList<String>(array1);
        assertEquals(array1.size(), extendedList.size());
        int size = array1.size();
        for (String s : array2) {
            extendedList.add(s);
            assertEquals(++size, extendedList.size());
        }
    }

    @Test public void testGet() {
        List<String> extendedList = new ExtendedList<String>(array1);
        for (int i = 0; i < array1.size(); ++i)
            assertEquals(array1.get(i), extendedList.get(i));

        for (String s : array2)
            extendedList.add(s);

        for (int i = 0; i < array1.size(); ++i)
            assertEquals(array1.get(i), extendedList.get(i));

        for (int i = 0; i < array2.size(); ++i)
            assertEquals(array2.get(i), extendedList.get(i + array1.size()));
    }

    @Test public void testIterator() {
        List<String> extendedList = new ExtendedList<String>(array1);
        for (String s : array2)
            extendedList.add(s);

        Iterator<String> comboIter = extendedList.iterator();
        Iterator<String> iter1 = array1.iterator();
        Iterator<String> iter2 = array2.iterator();

        while (iter1.hasNext()) {
            assertTrue(comboIter.hasNext());
            assertEquals(iter1.next(), comboIter.next());
        }

        while (iter2.hasNext()) {
            assertTrue(comboIter.hasNext());
            assertEquals(iter2.next(), comboIter.next());
        }
    }
}
