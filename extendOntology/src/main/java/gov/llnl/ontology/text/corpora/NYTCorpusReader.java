

package gov.llnl.ontology.text.corpora;

import gov.llnl.ontology.text.Document;

import java.util.Iterator;


/**
 * This {@link NYTCorpusReader} iterates over the contents of a single document.
 *
 * @author Keith Stevens
 */
public class NYTCorpusReader implements Iterator<Document> {

    /**
     * The {@link NYTCorpusDocument} to return.
     */
    private NYTCorpusDocument next;

    /**
     * Constructs a {@link NYTCorpusReader} that will iterate over the contents
     * of the given document in {@code doc}.
     */
    public NYTCorpusReader(String doc) {
        next = NYTCorpusDocumentParser.parseNYTCorpusDocumentFromString(
                doc, false);
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNext() {
        return next != null;
    }

    /**
     * {@inheritDoc}
     */
    public Document next() {
        Document doc = next;
        next = null;
        return doc;
    }
    
    /**
     * @throws UnsupportedOperationException
     */
    public void remove() {
        throw new UnsupportedOperationException(
                "Cannot remove from a NYTCorpusReader");
    }
}
