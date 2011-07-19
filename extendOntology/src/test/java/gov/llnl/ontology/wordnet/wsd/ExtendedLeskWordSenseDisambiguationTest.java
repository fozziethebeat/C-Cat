package gov.llnl.ontology.wordnet.wsd;

import gov.llnl.ontology.text.Sentence;
import gov.llnl.ontology.util.AnnotationUtil;
import gov.llnl.ontology.wordnet.GenericMockReader;

import edu.stanford.nlp.pipeline.Annotation;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.List;


/**
 * @author Keith Stevens
 */
public class ExtendedLeskWordSenseDisambiguationTest
        extends LeskWordSenseDisambiguationTest {

    public static final String[][] SYNSET_DATA =
    {
        { "cat.n.2", "the the the the the", "N" },
        { "cat.n.3", "cute lady", "N" },
        { "cat.n.1", "fluffy cute pet ", "N" },
        { "brown.n.1", "a color", "N"},
        { "brown.n.2", "cut but ugly pet", "N" },
        { "chicken.n.1", "fluffy cute beast", "N" },
        { "chicken.n.2", "a the", "N" },
        { "like.n.1", "fluffy machine pet that is not cute", "" },
    };

    @Test public void testDisambiguation() {
        WordSenseDisambiguation wsdAlg =
            new ExtendedLeskWordSenseDisambiguation();
        List<Sentence> sentences = getSentences(TEST_SENTENCE, TEST_POS);
        wsdAlg.setup(new GenericMockReader(SYNSET_DATA));
        wsdAlg.disambiguate(sentences);

        assertEquals(1, sentences.size());
        boolean foundCat = false;
        for (Annotation annot : sentences.get(0)) {
            if (AnnotationUtil.word(annot).equals("cat")) {
                foundCat = true;
                assertEquals(SYNSET_DATA[2][0], 
                             AnnotationUtil.wordSense(annot));
            }
        }
        assertTrue(foundCat);
    }
}

