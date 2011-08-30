package gov.llnl.document.preprocess;



/** 
 * Classes that implement the {@link Preprocessor} interface must be capable of transforming a document
 * into a simple plaintext file, where each sentence is a line in the document.
 *
 * @author Terry Huang
 */
    
public interface Preprocessor {

    public void preprocess();

}