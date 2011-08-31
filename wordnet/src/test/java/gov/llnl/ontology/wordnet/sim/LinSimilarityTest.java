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
import gov.llnl.ontology.wordnet.InformationContent;
import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;
import gov.llnl.ontology.wordnet.Synset.Relation;
import gov.llnl.ontology.wordnet.SynsetSimilarity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class LinSimilarityTest {

    @Test public void testNoSubsumer() {
        MockIC ic = new MockIC();

        Synset s1 = new BaseSynset(PartsOfSpeech.NOUN);
        Synset s2 = new BaseSynset(PartsOfSpeech.NOUN);

        SynsetSimilarity sim = new LinSimilarity(ic);
        assertEquals(0, sim.similarity(s1, s2), .00001);
    }

    @Test public void testSimilarity() {
        MockIC ic = new MockIC();

        Synset s1 = new BaseSynset(PartsOfSpeech.NOUN);
        ic.ic.put(s1, 2.0);
        Synset s2 = new BaseSynset(PartsOfSpeech.NOUN);
        ic.ic.put(s2, 3.0);
        
        Synset worst = new BaseSynset(PartsOfSpeech.NOUN);
        s1.addRelation(Relation.HYPERNYM, worst);
        s2.addRelation(Relation.HYPERNYM, worst);

        Synset best = new BaseSynset(PartsOfSpeech.NOUN);
        s1.addRelation(Relation.HYPERNYM, best);
        s2.addRelation(Relation.HYPERNYM, best);
        ic.ic.put(best, 10.0);

        Synset med = new BaseSynset(PartsOfSpeech.NOUN);
        s1.addRelation(Relation.HYPERNYM, med);
        s2.addRelation(Relation.HYPERNYM, med);
        ic.ic.put(med, 5.0);

        SynsetSimilarity sim = new LinSimilarity(ic);
        assertEquals(2*10/(5), sim.similarity(s1, s2), .0001);

        assertEquals(5, ic.seen.size());
        assertTrue(ic.seen.contains(med));
        assertTrue(ic.seen.contains(worst));
        assertTrue(ic.seen.contains(best));
        assertTrue(ic.seen.contains(s1));
        assertTrue(ic.seen.contains(s2));
    }

    public class MockIC implements InformationContent {
        Map<Synset, Double> ic;
        Set<Synset> seen;

        public MockIC() {
            ic = new HashMap<Synset, Double>();
            seen = new HashSet<Synset>();
        }

        public double contentForSynset(Synset s) {
            return -1;
        }

        public double contentForPartOfSpeech(PartsOfSpeech p) {
            return -1;
        }

        public double informationContent(Synset s) {
            seen.add(s);
            Double d = ic.get(s);
            return (d == null) ? -1 : d;
        }
    }
}
