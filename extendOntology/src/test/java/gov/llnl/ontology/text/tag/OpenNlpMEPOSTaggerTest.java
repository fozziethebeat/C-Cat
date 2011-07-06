package gov.llnl.ontology.text.tag;

import opennlp.tools.postag.POSTagger;


/**
 * @author Keith Stevens
 */
public class OpenNlpMEPOSTaggerTest extends POSTaggerTestBase {

    private static final String LOCAL_MODEL =
        "data/" + OpenNlpMEPOSTagger.DEFAULT_MODEL;

    protected POSTagger tagger(boolean fromJar) {
        return (fromJar)
            ? new OpenNlpMEPOSTagger()
            : new OpenNlpMEPOSTagger(LOCAL_MODEL, false);
    }
}
