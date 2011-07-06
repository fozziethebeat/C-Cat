package gov.llnl.ontology.text.sentsplit;

import opennlp.tools.util.Span;
import opennlp.tools.sentdetect.SentenceDetector;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public abstract class SentenceDetectorTestBase {

    public static final String TEST_PARAGRAPH =
        "the quick brown fox jumped over. Then something awesome happened.";

    @Test
    public void testSentenceDetector() {
        SentenceDetector detector = detector(false);
        String[] sentences = detector.sentDetect(TEST_PARAGRAPH);
        assertFalse(0 == sentences.length);
        for (String sentence : sentences) {
            assertNotNull(sentence);
            assertFalse("".equals(sentence));
        }
    }

    @Test
    public void testSentenceDetectorFromJar() {
        SentenceDetector detector = detector(true);
        String[] sentences = detector.sentDetect(TEST_PARAGRAPH);
        assertFalse(0 == sentences.length);
        for (String sentence : sentences) {
            assertNotNull(sentence);
            assertFalse("".equals(sentence));
        }
    }

    @Test
    public void testSpan() {
        SentenceDetector detector = detector(false);
        Span[] spans = detector.sentPosDetect(TEST_PARAGRAPH);
        for (Span span : spans) {
            assertNotNull(span);
            assertTrue(-1 < span.getStart());
            assertTrue(span.getStart() < TEST_PARAGRAPH.length());
            assertTrue(-1 < span.getEnd());
            assertTrue(span.getEnd() <= TEST_PARAGRAPH.length());
        }
    }

    @Test
    public void testSpanWithJar() {
        SentenceDetector detector = detector(true);
        Span[] spans = detector.sentPosDetect(TEST_PARAGRAPH);
        for (Span span : spans) {
            assertNotNull(span);
            assertTrue(-1 < span.getStart());
            assertTrue(span.getStart() < TEST_PARAGRAPH.length());
            assertTrue(-1 < span.getEnd());
            assertTrue(span.getEnd() <= TEST_PARAGRAPH.length());
        }
    }

    protected abstract SentenceDetector detector(boolean fromJar);
}
