

package gov.llnl.ontology.wordnet;

import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.*;


/**
 * @author Keith Stevens
 */
public class BaseLemmaTest {
    @Test public void testBasicAccessors() {
        Synset synset = new BaseSynset(PartsOfSpeech.NOUN);
        Lemma lemma = new BaseLemma(synset, "cat", "animal", 0, 1, "n");

        assertEquals(synset, lemma.getSynset());
        assertEquals("cat", lemma.getLemmaName());
        assertEquals("animal", lemma.getLexicographerName());
        assertEquals(0, lemma.getLexNameIndex());
        assertEquals(1, lemma.getLexicalId());
    }

    @Test public void testKey() {
        Synset synset = new BaseSynset(PartsOfSpeech.NOUN);
        BaseLemma lemma = new BaseLemma(synset, "cat", "animal", 0, 1, "n");

        lemma.setKey("key");
        assertEquals("key", lemma.getKey());
    }
}
