package gov.llnl.ontology.text.parse;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.*;
import java.util.jar.*;
import gov.llnl.ontology.util.StreamUtil;


/**
 * @author Keith Stevens
 */
public class MaltParserTest {

    public static final String TEST_SENT =
        "The quick brown fox jumped over the lazy dog.";

    @Test
    public void testParser() {
        Parser parser = new MaltParser();
        String parse = parser.parseText("", TEST_SENT);
        String[] lines = parse.split("\\n");
        assertEquals(10, lines.length);
        int i = 0;
        for (String line : lines) {
            String[] toks = line.split("\\s+");
            assertEquals(8, toks.length);
            assertEquals(i++, Integer.parseInt(toks[0]));
            assertFalse("_".equals(toks[1]));
            assertFalse("_".equals(toks[3]));
            assertFalse("_".equals(toks[4]));
            assertFalse("_".equals(toks[7]));
            int parentNum = Integer.parseInt(toks[6]);
            assertTrue(-1 < parentNum);
            assertTrue(parentNum <= lines.length);
        }
    }
}

