package gov.llnl.ontology.wordnet.wsd;

import gov.llnl.ontology.text.Sentence;
import gov.llnl.ontology.util.AnnotationUtil;
import gov.llnl.ontology.wordnet.LinkedMockReader;

import edu.stanford.nlp.pipeline.Annotation;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.List;


/**
 * @author Keith Stevens
 */
public class DegreeCentralityDisambiguationTest
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

    public static final String[][] SYNSET_LINKS =
    {
        { "cat.n.1", "brown.n.1" },
        { "cat.n.1", "brown.n.2" },
        { "cat.n.1", "chicken.n.1" },
        { "cat.n.1", "chicken.n.2" },
        { "cat.n.2", "chicken.n.2" },
        { "cat.n.3", "brown.n.2" },
    };

    @Test public void testDisambiguation() {
        WordSenseDisambiguation wsdAlg = new DegreeCentralityDisambiguation();
        List<Sentence> sentences = getSentences(TEST_SENTENCE, TEST_POS);
        LinkedMockReader reader = new LinkedMockReader(SYNSET_DATA);
        for (String[] synsetLink : SYNSET_LINKS)
            reader.connectSynsets(synsetLink[0], synsetLink[1], "r");

        wsdAlg.setup(reader);
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
