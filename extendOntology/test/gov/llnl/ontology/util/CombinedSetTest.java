

package gov.llnl.ontology.util;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.*;


/**
 * @author Keith Stevens
 */
public class CombinedSetTest {

    private static final String[] array1 = 
        {"one", "two", "three"};

    private static final String[] array2 = 
        {"one", "two", "three"};

    @Test public void testCombinedSize() {
        Set<String> set1 = new HashSet<String>();
        for (String s : array1)
            set1.add(s);
        Set<String> set2 = new HashSet<String>();
        for (String s : array2)
            set2.add(s);

        Set<String> combined = new CombinedSet<String>(set1, set2);
        assertEquals(6, combined.size());
    }

    @Test public void testContains() {
        Set<String> set1 = new HashSet<String>();
        for (String s : array1)
            set1.add(s);
        Set<String> set2 = new HashSet<String>();
        for (String s : array2)
            set2.add(s);

        Set<String> combined = new CombinedSet<String>(set1, set2);
        for (String s : array1)
            assertTrue(combined.contains(s));
        for (String s : array2)
            assertTrue(combined.contains(s));
    }

    @Test public void testIterator() {
        Set<String> set1 = new HashSet<String>();
        for (String s : array1)
            set1.add(s);
        Set<String> set2 = new HashSet<String>();
        for (String s : array2)
            set2.add(s);

        Set<String> combined = new CombinedSet<String>(set1, set2);
        Iterator<String> comboIter = combined.iterator();
        Iterator<String> iter1 = set1.iterator();
        Iterator<String> iter2 = set2.iterator();

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
