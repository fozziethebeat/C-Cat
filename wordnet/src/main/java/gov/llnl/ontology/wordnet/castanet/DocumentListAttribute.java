package gov.llnl.ontology.wordnet.castanet;

import gov.llnl.ontology.text.Document;
import gov.llnl.ontology.wordnet.Attribute;

import com.google.common.collect.Lists;

import java.util.List;


/**
 * @author Keith Stevens
 */
public class DocumentListAttribute implements Attribute<List<Document>> {

    private final List<Document> docList;

    public DocumentListAttribute() {
        docList = Lists.newArrayList();
    }

    public void merge(Attribute<List<Document>> other) {
        docList.addAll(other.object());
    }

    public List<Document> object() {
        return docList;
    }
}
