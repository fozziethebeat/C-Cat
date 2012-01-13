package gov.llnl.ontology.text;

import java.util.Iterator;


/**
 * @author Keith Stevens
 */
public interface CorpusReader extends Iterator<Document> {

    void initialize();
}
