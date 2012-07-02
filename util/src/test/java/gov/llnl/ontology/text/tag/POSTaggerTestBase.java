package gov.llnl.ontology.text.tag;

import opennlp.tools.postag.POSTagger;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public abstract class POSTaggerTestBase {

    public static final String[] TEST_SENT =
    { "the", "quick", "brown", "fox", "jumped", "over", "the", "lazy", "dog", "." };

    @Test
    public void testPOSTaggerFromJar() {
        POSTagger tagger = tagger(true);
        String[] tags = tagger.tag(TEST_SENT);
        assertFalse(0 == tags.length);
        for (String tag : tags) {
            assertNotNull(tag);
            assertFalse("".equals(tag));
        }
    }

    protected abstract POSTagger tagger(boolean fromJar);
}
