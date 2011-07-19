package gov.llnl.ontology.wordnet;


/**
 * @author Keith Stevens
 */
public class LinkedMockReader extends GenericMockReader {

    public LinkedMockReader(String[][] synsetData) {
        super(synsetData);
    }

    public void connectSynsets(String lemma1, String lemma2, String rel) {
        Synset s1 = getSynset(lemma1);
        Synset s2 = getSynset(lemma2);
        s1.addRelation(rel, s2);
        s2.addRelation(rel, s1);
    }
}
