package gov.llnl.ontology.text.tokenize;

import opennlp.tools.tokenize.Tokenizer;


/**
 * @author Keith Stevens
 */
public class OpenNlpMETokenizerTest extends TokenizerTestBase {

    private static final String LOCAL_MODEL =
        "data/" + OpenNlpMETokenizer.DEFAULT_MODEL;

    protected Tokenizer tokenizer(boolean fromJar) {
        return (fromJar)
            ? new OpenNlpMETokenizer()
            : new OpenNlpMETokenizer(LOCAL_MODEL, false);
    }
}
