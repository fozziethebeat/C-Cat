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

    /*
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
    */

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

    /*
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
    */

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
