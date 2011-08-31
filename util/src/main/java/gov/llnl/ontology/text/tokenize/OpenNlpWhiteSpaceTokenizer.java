package gov.llnl.ontology.text.tokenize;

import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.WhitespaceTokenizer;


/**
 * A decorator for instantiating a {@link WhitespaceTokenizer} using the
 * standard reflection framework.  This simply adapts the {@link
 * WhitespaceTokenizer} by loading one from the {@link
 * WhitespaceTokenizer#INSTANCE} static reference.
 *
 * @author Keith Stevens
 */
public class OpenNlpWhiteSpaceTokenizer extends TokenizerAdaptor {

    /**
     * Creates a new {@link OpenNlpWhiteSpaceTokenizer}.
     */
    public OpenNlpWhiteSpaceTokenizer() {
        super(WhitespaceTokenizer.INSTANCE);
    }
}
