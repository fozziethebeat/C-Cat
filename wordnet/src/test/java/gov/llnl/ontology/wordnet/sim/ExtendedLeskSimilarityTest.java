package gov.llnl.ontology.wordnet.sim;

import gov.llnl.ontology.wordnet.BaseSynset;
import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;
import gov.llnl.ontology.wordnet.Synset.Relation;
import gov.llnl.ontology.wordnet.SynsetSimilarity;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class ExtendedLeskSimilarityTest {
    @Test public void testLeskSimilarity() {
        Synset s1 = new BaseSynset(PartsOfSpeech.NOUN);
        Synset s2 = new BaseSynset(PartsOfSpeech.NOUN);
        Synset p2 = new BaseSynset(PartsOfSpeech.NOUN);

        s1.setDefinition("how now brown cow");
        s2.setDefinition("how now sad meow");
        p2.setDefinition("cow sad sad");
        s2.addRelation(Relation.HYPERNYM, p2);

        SynsetSimilarity sim = new ExtendedLeskSimilarity();
        assertEquals(6, sim.similarity(s1, s2), .00001);
    }
}
