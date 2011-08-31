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



package gov.llnl.ontology.wordnet;

import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.*;


/**
 * @author Keith Stevens
 */
public class BaseLemmaTest {
    @Test public void testBasicAccessors() {
        Synset synset = new BaseSynset(PartsOfSpeech.NOUN);
        Lemma lemma = new BaseLemma(synset, "cat", "animal", 0, 1, "n");

        assertEquals(synset, lemma.getSynset());
        assertEquals("cat", lemma.getLemmaName());
        assertEquals("animal", lemma.getLexicographerName());
        assertEquals(0, lemma.getLexNameIndex());
        assertEquals(1, lemma.getLexicalId());
    }

    @Test public void testKey() {
        Synset synset = new BaseSynset(PartsOfSpeech.NOUN);
        BaseLemma lemma = new BaseLemma(synset, "cat", "animal", 0, 1, "n");

        lemma.setKey("key");
        assertEquals("key", lemma.getKey());
    }
}
