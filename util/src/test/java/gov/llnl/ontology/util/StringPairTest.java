package gov.llnl.ontology.util;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;


/**
 * @author Keith Stevens
 */
public class StringPairTest {

    @Test public void testCreator() {
        StringPair pair = new StringPair("cat", "dog");
        assertEquals("cat", pair.x);
        assertEquals("dog", pair.y);
    }

    @Test public void testEmptyCreator() {
        StringPair pair = new StringPair();
        assertNull(pair.x);
        assertNull(pair.y);
    }

    @Test public void testCompareToFirstDiffer() {
        StringPair pair1 = new StringPair("cat", "alpha");
        StringPair pair2 = new StringPair("cats", "dog");
        assertTrue(pair1.compareTo(pair2) < 0);
    }

    @Test public void testCompareToFirstEqual() {
        StringPair pair1 = new StringPair("cat", "alpha");
        StringPair pair2 = new StringPair("cat", "dog");
        assertTrue(pair1.compareTo(pair2) < 0);
    }

    @Test public void testCompareToBothEqual() {
        StringPair pair1 = new StringPair("cat", "dog");
        StringPair pair2 = new StringPair("cat", "dog");
        assertTrue(pair1.compareTo(pair2) == 0);
    }

    @Test public void testEqualsNull() {
        StringPair pair1 = new StringPair();
        StringPair pair2 = new StringPair();
        assertTrue(pair1.equals(pair2));
    }

    @Test public void testEqualsBothMatch() {
        StringPair pair1 = new StringPair("cat", "dog");
        StringPair pair2 = new StringPair("cat", "dog");
        assertTrue(pair1.equals(pair2));
    }

    @Test public void testEqualsBothDiffer() {
        StringPair pair1 = new StringPair("cat", "dog");
        StringPair pair2 = new StringPair("dog", "cat");
        assertFalse(pair1.equals(pair2));
    }

    @Test public void testEqualsFirstMatchSecondNull() {
        StringPair pair1 = new StringPair("cat", null);
        StringPair pair2 = new StringPair("cat", null);
        assertTrue(pair1.equals(pair2));
    }

    @Test public void testEqualsFirstMatchSecondDiffer() {
        StringPair pair1 = new StringPair("cat", "cat");
        StringPair pair2 = new StringPair("cat", "c");
        assertFalse(pair1.equals(pair2));
    }

    @Test public void testEqualsFirstNullSecondMatch() {
        StringPair pair1 = new StringPair(null, "cat");
        StringPair pair2 = new StringPair(null, "cat");
        assertTrue(pair1.equals(pair2));
    }

    @Test public void testEqualsFirstNullSecondDiffer() {
        StringPair pair1 = new StringPair(null, "c");
        StringPair pair2 = new StringPair(null, "cat");
        assertFalse(pair1.equals(pair2));
    }

    @Test public void testWriteReadFields() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        StringPair pair = new StringPair("cat", "dog");
        pair.write(dos);

        ByteArrayInputStream bais = new ByteArrayInputStream(
                baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        pair = new StringPair();
        pair.readFields(dis);
        assertEquals("cat", pair.x);
        assertEquals("dog", pair.y);
    }

    @Test public void testToString() {
        StringPair pair = new StringPair("c,at", "dog,");
        assertEquals("{c&comma;at, dog&comma;}", pair.toString());
    }

    @Test public void testFromString() {
        StringPair pair = StringPair.fromString(
                "{c&comma;at, dog&comma;}");
        assertEquals("c,at", pair.x);
        assertEquals("dog,", pair.y);
    }
}
