package gov.llnl.ontology.wordnet;


/**
 * An interface for saving a Wordnet ontology in a unique format.  Each {@link
 * OntologyWriter} implementation should specify it's serialized format.  
 *
 * @author Keith Stevens
 */
public interface OntologyWriter {

    /**
     * Saves all of the {@link Synset}s mapped by {@code reader} into dictionary
     * files based in the directory designated by {@code outputDir}.
     */
    void saveOntology(OntologyReader reader, String outputDir);
}
