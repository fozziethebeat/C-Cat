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
import gov.llnl.ontology.wordnet.OntologyReaderAdaptor;
import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;
import gov.llnl.ontology.wordnet.Synset.Relation;
import gov.llnl.ontology.wordnet.SynsetSimilarity;
import gov.llnl.ontology.wordnet.UnsupportedOntologyReader;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class LeacockChodorowSimilarityTest {

    @Test public void testWrongPos() {
        Synset s1 = new BaseSynset(PartsOfSpeech.NOUN);
        Synset s2 = new BaseSynset(PartsOfSpeech.VERB);

        SynsetSimilarity sim = new LeacockChodorowSimilarity(null);
        assertEquals(0, sim.similarity(s1, s2), .0001);
    }

    public static void addParents(Synset s1, Synset s2) {
        int[][] depths = { {4, 2}, {3, 1} };

        for (int k = 0; k < depths.length; ++k) {
            Synset subsumer = new BaseSynset(PartsOfSpeech.NOUN);
            Synset parent = subsumer;
            for (int i = 0; i < depths[k][0]; ++i) {
                Synset child = new BaseSynset(PartsOfSpeech.NOUN);
                child.addRelation(Relation.HYPERNYM, parent);
                parent = child;
            }
            s1.addRelation(Relation.HYPERNYM, parent);

            parent = subsumer;
            for (int i = 0; i < depths[k][1]; ++i) {
                Synset child = new BaseSynset(PartsOfSpeech.NOUN);
                child.addRelation(Relation.HYPERNYM, parent);
                parent = child;
            }
            s2.addRelation(Relation.HYPERNYM, parent);
        }
    }

    @Test public void testSimilarity() {
        Synset s1 = new BaseSynset(PartsOfSpeech.NOUN);
        Synset s2 = new BaseSynset(PartsOfSpeech.NOUN);

        addParents(s1, s2);

        MockReader reader = new MockReader();
        SynsetSimilarity sim = new LeacockChodorowSimilarity(reader);
        double expected = -1 * Math.log(7 / (2d * 1));
        assertEquals(expected, sim.similarity(s1, s2), .0001);
    }

    @Test public void testSimilarityVerb() {
        Synset s1 = new BaseSynset(PartsOfSpeech.VERB);
        Synset s2 = new BaseSynset(PartsOfSpeech.VERB);

        addParents(s1, s2);

        MockReader reader = new MockReader();
        SynsetSimilarity sim = new LeacockChodorowSimilarity(reader);
        double expected = -1 * Math.log(7 / (2d * 2));
        assertEquals(expected, sim.similarity(s1, s2), .0001);
    }

    public class MockReader extends OntologyReaderAdaptor {
        
        int[] depths = {1, 2, 4, 5, 6};

        public MockReader() {
            super(new UnsupportedOntologyReader());
        }
        
        public int getMaxDepth(PartsOfSpeech pos) {
            if (pos == PartsOfSpeech.NOUN)
                return depths[0];
            if (pos == PartsOfSpeech.VERB)
                return depths[1];
            if (pos == PartsOfSpeech.ADJECTIVE)
                return depths[2];
            if (pos == PartsOfSpeech.ADVERB)
                return depths[3];
            return depths[4];
        }
    }
}

