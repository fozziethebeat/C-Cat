package gov.llnl.ontology.text.tokenize;

import opennlp.tools.util.Span;
import opennlp.tools.tokenize.Tokenizer;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public abstract class TokenizerTestBase {

    public static final String TEST_SENT = "the quick; brown fox, jumped over.";

    @Test
    public void testTokenizer() {
        Tokenizer tokenizer = tokenizer(false);
        String[] tokens = tokenizer.tokenize(TEST_SENT);
        assertFalse(0 == tokens.length);
        for (String token : tokens) {
            assertNotNull(token);
            assertFalse("".equals(token));
        }
    }

    @Test
    public void testTokenizerFromJar() {
        Tokenizer tokenizer = tokenizer(true);
        String[] tokens = tokenizer.tokenize(TEST_SENT);
        assertFalse(0 == tokens.length);
        for (String token : tokens) {
            assertNotNull(token);
            assertFalse("".equals(token));
        }
    }

    @Test
    public void testSpan() {
        Tokenizer tokenizer = tokenizer(false);
        Span[] spans = tokenizer.tokenizePos(TEST_SENT);
        for (Span span : spans) {
            assertNotNull(span);
            assertTrue(-1 < span.getStart());
            assertTrue(span.getStart() < TEST_SENT.length());
            assertTrue(-1 < span.getEnd());
            assertTrue(span.getEnd() <= TEST_SENT.length());
        }
    }

    @Test
    public void testSpanWithJar() {
        Tokenizer tokenizer = tokenizer(true);
        Span[] spans = tokenizer.tokenizePos(TEST_SENT);
        for (Span span : spans) {
            assertNotNull(span);
            assertTrue(-1 < span.getStart());
            assertTrue(span.getStart() < TEST_SENT.length());
            assertTrue(-1 < span.getEnd());
            assertTrue(span.getEnd() <= TEST_SENT.length());
        }
    }

    protected abstract Tokenizer tokenizer(boolean fromJar);
}
