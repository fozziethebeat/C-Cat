

package gov.llnl.ontology.text;


/**
 * A simple struct based implementation of a {@link Document}.
 * @author Keith Stevens
 */
public class SimpleDocument implements Document {

    /**
     * The corpus form which this {@link SimpleDocument} came.
     */
    private final String corpusName;

    /**
     * The cleaned string text for this {@link SimpleDocument}.
     */
    private final String docText;

    /**
     * The unique string key for this {@link SimpleDocument}.
     */
    private final String key;

    /**
     * The unique integer id for this {@link SimpleDocument}.
     */
    private final long id;

    /**
     * The string title for this {@link SimpleDocument}.
     */
    private final String title;

    /**
     * Constructs a new {@link SimpleDocument} using the given data values.
     *
     * @param corpusName the name of the corpus that this document came from
     * @param docText the cleaned text for this document
     * @param key A string based key for this document
     * @param id A unique identifier for this key
     * @param title A title for the document
     */
    public SimpleDocument(String corpusName, 
                          String docText, 
                          String key, 
                          long id, 
                          String title) {
        this.corpusName = corpusName;
        this.docText = docText;
        this.key = key;
        this.id = id;
        this.title = title;
    }

    /**
     * {@inheritDoc}
     */
    public String sourceCorpus() {
        return corpusName;
    }

    /**
     * {@inheritDoc}
     */
    public String rawText() {
        return docText;
    }

    /**
     * {@inheritDoc}
     */
    public String key() {
        return key;
    }

    /**
     * {@inheritDoc}
     */
    public long id() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    public String title() {
        return title;
    }
}
