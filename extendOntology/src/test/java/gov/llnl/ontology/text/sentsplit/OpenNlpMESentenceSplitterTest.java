package gov.llnl.ontology.text.sentsplit;

import opennlp.tools.sentdetect.SentenceDetector;


/**
 * @author Keith Stevens
 */
public class OpenNlpMESentenceSplitterTest extends SentenceDetectorTestBase {

    private static final String LOCAL_MODEL =
        "data/" + OpenNlpMESentenceSplitter.DEFAULT_MODEL;

    protected SentenceDetector detector(boolean fromJar) {
        return (fromJar)
            ? new OpenNlpMESentenceSplitter()
            : new OpenNlpMESentenceSplitter(LOCAL_MODEL, false);
    }
}
