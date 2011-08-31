package gov.llnl.ontology.wordnet.wsd;

import gov.llnl.ontology.text.Sentence;
import gov.llnl.ontology.wordnet.OntologyReader;

import java.util.List;


/**
 * An interface for any Word Sense Disambiguation algorithm.  Implementations
 * will be given a list of {@link Sentence}s that need to be disambiguated.
 * Each disambiguated word should have it's {@link Annotation} updated with a
 * word sense tag corresponding to some {@link Synset} in WordNet.
 *
 * @author Keith Stevens
 */
public interface WordSenseDisambiguation {

    /**
     * Initializes the {@link WordSenseDisambiguation} algorithm with the given
     * {@link OntologyReader}.  Any other configuration values should be set via
     * the global system properties object.
     */
    void setup(OntologyReader reader);

    /**
     * Disambiguates each noun found in {@code sentences}.  The word sense name
     * will be added to the corresponding word's {@link Annotation} held within
     * {@code sentences}.
     */
    void disambiguate(List<Sentence> sentences);
}

