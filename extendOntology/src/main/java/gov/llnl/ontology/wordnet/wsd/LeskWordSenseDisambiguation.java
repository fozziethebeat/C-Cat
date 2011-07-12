/*
 * Copyright (c) 2011, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
 *
 * This file is part of the C-Cat package and is covered under the terms and
 * conditions therein.
 *
 * The S-Space package is free software: you can redistribute it and/or modify
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

package gov.llnl.ontology.wordnet.wsd;

import gov.llnl.ontology.text.Sentence;
import gov.llnl.ontology.util.AnnotationUtil;
import gov.llnl.ontology.wordnet.LeskSimilarity;
import gov.llnl.ontology.wordnet.OntologyReader;
import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;
import gov.llnl.ontology.wordnet.SynsetSimilarity;

import edu.stanford.nlp.pipeline.Annotation;

import edu.ucla.sspace.util.CombinedIterator;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;


/**
 * A {@link WordSenseDisambiguation} implementation using the {@link
 * LeskSimilarity} measure.  
 *
 * </p>
 *
 * This class <b>is</b> thread safe.
 *
 * @author Keith Stevens
 */
public class LeskWordSenseDisambiguation extends SlidingWindowDisambiguation {

    protected OntologyReader reader;

    protected SynsetSimilarity sim;

    public void setup(OntologyReader reader) {
        this.reader = reader;
        this.sim = new LeskSimilarity();
    }

    protected void processContext(Annotation focus,
                                  Queue<Annotation> prevWords,
                                  Queue<Annotation> nextWords) {
        Synset[] focusSynsets = reader.getSynsets(
                AnnotationUtil.word(focus), PartsOfSpeech.NOUN);
        double[] synsetScores = new double[focusSynsets.length];

        for (Annotation prev : prevWords)
            computeScore(synsetScores, focusSynsets, prev);
        for (Annotation next : nextWords)
            computeScore(synsetScores, focusSynsets, next);

        double maxScore = 0;
        int maxId = 0;
        for (int i = 0; i < synsetScores.length; ++i) 
            if (synsetScores[i] > maxScore) {
                maxScore = synsetScores[i];
                maxId = i;
            }

        AnnotationUtil.setWordSense(focus, focusSynsets[maxId].getName());
    }

    private void computeScore(double[] synsetScores,
                              Synset[] focusSynsets,
                              Annotation prev) {
        Synset[] others = reader.getSynsets(
                AnnotationUtil.word(prev), PartsOfSpeech.NOUN);
        for (int i = 0; i < focusSynsets.length; ++i)
            for (Synset other : others)
                synsetScores[i] += sim.similarity(focusSynsets[i], other);
    }
}
