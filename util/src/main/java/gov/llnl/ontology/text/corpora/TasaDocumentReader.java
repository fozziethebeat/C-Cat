package gov.llnl.ontology.text.corpora;

import gov.llnl.ontology.text.Document;
import gov.llnl.ontology.text.DocumentReader;
import gov.llnl.ontology.text.SimpleDocument;


/**
 * @author Keith Stevens
 */
public class TasaDocumentReader implements DocumentReader {

    public Document readDocument(String doc) {
        return readDocument(doc, "Touchstone Applied Science Associates");
    }

    public Document readDocument(String doc, String corpusName) {
        String[] headerText = doc.split("\\n", 2);
        String header = headerText[0].replaceAll("[A-Za-z]+=", "");
        header = header.replaceAll("[<>]", "");
        String[] keyId = header.split("\\s+");
        return new SimpleDocument(corpusName, headerText[1], doc, 
                                  keyId[0], 0, keyId[2], null);
    }
}
