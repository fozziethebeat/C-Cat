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

import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.SynsetSimilarity;
import gov.llnl.ontology.util.StringUtils;

import com.google.common.collect.Sets;

import java.util.Set;


/**
 * @author Keith Stevens
 */
public class ExtendedLeskSimilarity implements SynsetSimilarity {

    /**
     * {@inheritDoc}
     */
    public double similarity(Synset synset1, Synset synset2) {
        Set<Synset> synsets1 = Sets.newHashSet(synset1.allRelations()); 
        synsets1.add(synset1);
        Set<Synset> synsets2 = Sets.newHashSet(synset2.allRelations());
        synsets2.add(synset2);

        double score = 0;
        for (Synset s1 : synsets1) 
            for (Synset s2 : synsets2)
                score += score(s1.getGloss(), s2.getGloss());
        return score;
    }

    /**
     * Computes the gloss overlap between two {@link Synset}s when splitting the
     * tokens using whitespace.
     */
    private static double score(String gloss1, String gloss2) {
        String[] gTokens1 = gloss1.split("\\s+");
        String[] gTokens2 = gloss2.split("\\s+");
        return StringUtils.tokenOverlapExp(gTokens1, gTokens2);
    }
}
