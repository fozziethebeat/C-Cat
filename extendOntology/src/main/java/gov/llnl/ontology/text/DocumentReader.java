

package gov.llnl.ontology.text;


/**
 * {@link DocumentReader}s transform a string of text into a raw document.  The
 * original text may contain xml or other formatting information and the cleaned
 * version contains just the interesting text.
 *
 * @author Keith Stevens
 */
public interface DocumentReader {

    /**
     * Returns a {@link Document} represented by the given string.
     */
    Document readDocument(String doc);
}
