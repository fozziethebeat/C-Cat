

package gov.llnl.ontology.wordnet;

import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class WordNetCorpusReaderTest {

    @Test public void testSynsetMerge() {
        OntologyReader reader = WordNetCorpusReader.initialize("dict/");

        Synset first = reader.getSynset("cat", PartsOfSpeech.NOUN, 1);
        Synset second = reader.getSynset("feline", PartsOfSpeech.NOUN, 1);
        first.merge(second);
        System.out.println(first.getParentPaths());
    }
}
