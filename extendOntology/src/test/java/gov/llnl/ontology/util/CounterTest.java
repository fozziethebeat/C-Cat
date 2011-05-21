

package gov.llnl.ontology.util;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.*;


/**
 * @author Keith Stevens
 */
public class CounterTest {
    @Test public void testItemsSortedAscending() {
        Counter<String> counter = new Counter();
        counter.count("cat");
        counter.count("cat");
        counter.count("dog");

        List<String> items = counter.itemsSorted(true);
        assertEquals(2, items.size());
        assertEquals("dog", items.get(0));
        assertEquals("cat", items.get(1));
    }

    @Test public void testItemsSortedDescending() {
        Counter<String> counter = new Counter();
        counter.count("cat");
        counter.count("cat");
        counter.count("dog");

        List<String> items = counter.itemsSorted(false);
        assertEquals(2, items.size());
        assertEquals("cat", items.get(0));
        assertEquals("dog", items.get(1));
    }
}
