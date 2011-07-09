

package gov.llnl.ontology.wordnet;


/**
 * An interface for determining the similarity between two {@link Synset}s.
 * Implementations are the same measures provided by the perl <a
 * href="http://wn-similarity.sourceforge.net/">WordNet::Similarity</a> package
 * distributed by Ted Pedersen.
 *
 * </p>
 *
 * The similarity measures come in three varieties: measures based on the paths
 * between two synsets, measures based on information content for each synset,
 * and a combination of path information and information content.
 *
 * </p>
 *
 * Implementations should take a {@link OntologyReader} as an argument to the
 * contructor and may take other arguments via the constructor, such as a {@link
 * InformationContent} instance.  All calls to {@link similarity(Synset, Synset)
 * similarity} should be free of state changes and thus thread safe.
 *
 * @author Keith Stevens
 */
public interface SynsetSimilarity {

    /**
     * Returns the similarity between {@link synset1} and {@link synset2} based
     * on how they are connected in the word net hierarchy.
     */
    double similarity(Synset synset1, Synset synset2);
}
