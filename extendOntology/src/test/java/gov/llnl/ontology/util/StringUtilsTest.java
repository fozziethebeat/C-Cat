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

package gov.llnl.ontology.util;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class StringUtilsTest {

    @Test public void testTokenOverlapDouble() {
        String[] tokens1 = {"the", "cat", "has", "a", "brown", "bag"};
        String[] tokens2 = {"bag", "dog", "was", "the", "blah", "bag"};
        assertEquals(2, StringUtils.tokenOverlap(tokens1, tokens2));
    }

    @Test public void testTokenOverlapSingle() {
        String[] tokens1 = {"the", "cat", "has", "a", "brown", "bag"};
        String[] tokens2 = {"chicken", "dog", "was", "the", "blah", "bag"};
        assertEquals(2, StringUtils.tokenOverlap(tokens1, tokens2));
    }

    @Test public void testTokenOverlapMixed() {
        String[] tokens1 = {"the", "cat", "has", "a", "brown", "bag"};
        String[] tokens2 = {"bag", "dog", "was", "cat", "blah", "bag"};
        assertEquals(2, StringUtils.tokenOverlap(tokens1, tokens2));
    }

    @Test public void testTokenOverlapExpNoSequence() {
        String[] tokens1 = {"the", "cat", "has", "a", "brown", "bag"};
        String[] tokens2 = {"bag", "dog", "was", "cat", "blah", "bag"};
        assertEquals(3, StringUtils.tokenOverlapExp(tokens1, tokens2));
    }

    @Test public void testTokenOverlapExpBoundedSequence() {
        String[] tokens1 = {"the", "cat", "has", "chicken", "bag", "the"};
        String[] tokens2 = {"the", "dog", "was", "blarg", "chicken", "bag"};
        assertEquals(7, StringUtils.tokenOverlapExp(tokens1, tokens2));
    }
}

