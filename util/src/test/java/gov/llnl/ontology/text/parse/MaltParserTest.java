/*
 * Copyright (c) 2011, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
 *
 * This file is part of the C-Cat package and is covered under the terms and
 * conditions therein.
 *
 * The C-Cat package is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation and distributed hereunder to you.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND NO REPRESENTATIONS OR WARRANTIES,
 * EXPRESS OR IMPLIED ARE MADE.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, WE MAKE
 * NO REPRESENTATIONS OR WARRANTIES OF MERCHANT- ABILITY OR FITNESS FOR ANY
 * PARTICULAR PURPOSE OR THAT THE USE OF THE LICENSED SOFTWARE OR DOCUMENTATION
 * WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER
 * RIGHTS.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

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
    public void testSvmParser() {
        /*
        Parser parser = new MaltSvmParser();
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
        */
    }

    @Test
    public void testLinearParser() {
        /*
        Parser parser = new MaltLinearParser();
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
        */
    }
}
