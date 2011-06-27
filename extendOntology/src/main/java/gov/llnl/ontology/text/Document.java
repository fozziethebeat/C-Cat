

package gov.llnl.ontology.text;


/**
 * An interface for representing a document.
 *
 * @author Keith Stevens
 */
public interface Document {

    /**
     * Returns the name of the source corpus.
     */
    String sourceCorpus();

    /**
     * Returns the raw text of the corpus.
     */
    String rawText();

    /**
     * Returns a string name of this document.
     */
    String key();

    /**
     * Returns a unique identifier for this document.
     */
    long id();

    /**
     * Returns the title of this document, if any exists.
     */
    String title();
}
