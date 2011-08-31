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

package gov.llnl.ontology.wordnet.sim;

import gov.llnl.ontology.wordnet.BaseSynset;
import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;
import gov.llnl.ontology.wordnet.Synset.Relation;
import gov.llnl.ontology.wordnet.SynsetSimilarity;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class WuPalmerSimilarityTest {

    @Test public void testNoSubsumer() {
        Synset s1 = new BaseSynset(PartsOfSpeech.NOUN);
        Synset s2 = new BaseSynset(PartsOfSpeech.NOUN);

        SynsetSimilarity sim = new WuPalmerSimilarity();
        assertEquals(0, sim.similarity(s1, s2), .00001);
    }

    @Test public void testNounSimilarity() {
        Synset subsumer = new BaseSynset(PartsOfSpeech.NOUN);

        Synset parent = subsumer;
        for (int i = 0; i < 4; ++i) {
            Synset child = new BaseSynset(PartsOfSpeech.NOUN);
            child.addRelation(Relation.HYPERNYM, parent);
            parent = child;
        }
        Synset s1 = new BaseSynset(PartsOfSpeech.NOUN);
        s1.addRelation(Relation.HYPERNYM, parent);

        parent = subsumer;
        for (int i = 0; i < 2; ++i) {
            Synset child = new BaseSynset(PartsOfSpeech.NOUN);
            child.addRelation(Relation.HYPERNYM, parent);
            parent = child;
        }
        Synset s2 = new BaseSynset(PartsOfSpeech.NOUN);
        s2.addRelation(Relation.HYPERNYM, parent);

        SynsetSimilarity sim = new WuPalmerSimilarity();
        double expected = (2.0 * 2) / (7 + 5);
        assertEquals(expected, sim.similarity(s1, s2), .00001);
    }

    @Test public void testVerbSimilarity() {
        Synset subsumer = new BaseSynset(PartsOfSpeech.VERB);

        Synset parent = subsumer;
        for (int i = 0; i < 4; ++i) {
            Synset child = new BaseSynset(PartsOfSpeech.NOUN);
            child.addRelation(Relation.HYPERNYM, parent);
            parent = child;
        }
        Synset s1 = new BaseSynset(PartsOfSpeech.NOUN);
        s1.addRelation(Relation.HYPERNYM, parent);

        parent = subsumer;
        for (int i = 0; i < 2; ++i) {
            Synset child = new BaseSynset(PartsOfSpeech.NOUN);
            child.addRelation(Relation.HYPERNYM, parent);
            parent = child;
        }
        Synset s2 = new BaseSynset(PartsOfSpeech.NOUN);
        s2.addRelation(Relation.HYPERNYM, parent);

        SynsetSimilarity sim = new WuPalmerSimilarity();
        double expected = (2.0 * 1) / (6 + 4);
        assertEquals(expected, sim.similarity(s1, s2), .00001);
    }
}
