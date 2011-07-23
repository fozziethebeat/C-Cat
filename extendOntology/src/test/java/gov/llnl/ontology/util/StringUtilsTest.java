package gov.llnl.ontology.util;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class StringUtilsTest {

    @Test public void testTokenOverlapDouble() {
        String[] tokens1 = {"the", "cat", "has", "a", "brown", "bag"};
        String[] tokens2 = {"bag", "dog", "was", "the", "blah", "bag"};
        assertEquals(2, StringUtils.tokenOverlap(tokens1, tokens2));
    }

    @Test public void testTokenOverlapSingle() {
        String[] tokens1 = {"the", "cat", "has", "a", "brown", "bag"};
        String[] tokens2 = {"chicken", "dog", "was", "the", "blah", "bag"};
        assertEquals(2, StringUtils.tokenOverlap(tokens1, tokens2));
    }

    @Test public void testTokenOverlapMixed() {
        String[] tokens1 = {"the", "cat", "has", "a", "brown", "bag"};
        String[] tokens2 = {"bag", "dog", "was", "cat", "blah", "bag"};
        assertEquals(2, StringUtils.tokenOverlap(tokens1, tokens2));
    }

    @Test public void testTokenOverlapExpNoSequence() {
        String[] tokens1 = {"the", "cat", "has", "a", "brown", "bag"};
        String[] tokens2 = {"bag", "dog", "was", "cat", "blah", "bag"};
        assertEquals(3, StringUtils.tokenOverlapExp(tokens1, tokens2));
    }

    @Test public void testTokenOverlapExpBoundedSequence() {
        String[] tokens1 = {"the", "cat", "has", "chicken", "bag", "the"};
        String[] tokens2 = {"the", "dog", "was", "blarg", "chicken", "bag"};
        assertEquals(7, StringUtils.tokenOverlapExp(tokens1, tokens2));
    }
}

