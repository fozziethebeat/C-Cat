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

import edu.stanford.nlp.pipeline.Annotation;

import edu.ucla.sspace.util.CombinedIterator;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;


/**
 * An abstract {@link WordSenseDisambiguation} implementation that uses a
 * sliding window of text over the document.  The focus word in each window will
 * be disambiguated if it is for a noun, and the word is found in WordNet,
 * otherwise the focus word will be ignored.  Subclasses should implement {@link
 * processContext} to handle each sliding window context and update the focus
 * word {@link Annotation} with a word sense tag.
 *
 * </p>
 *
 * This class <b>is</b> thread safe.
 *
 * @author Keith Stevens
 */
public abstract class SlidingWindowDisambiguation
        implements WordSenseDisambiguation {

    /**
     * Processes the local context surrounded {@code focus}.  This should update
     * {@code focus} with any word sense disambiguation {@link Annotation}s
     * discovered.
     *
     * @param focus The {@link Annotation} to disambiguate.
     * @param prevWords The N {@link Annotation} words before {@code focus}.
     * @param nextWords The N {@link Annotation} words after {@code focus}.
     */
    protected abstract void processContext(Annotation focus,
                                           Queue<Annotation> prevWords,
                                           Queue<Annotation> nextWords);

    /**
     * {@inheritDoc}
     */
    public void disambiguate(List<Sentence> sentences) {
        List<Iterator<Annotation>> annotationIterators =
            new ArrayList<Iterator<Annotation>>();
        for (Sentence sent : sentences)
            annotationIterators.add(sent.iterator());

        Iterator<Annotation> annotIter = new CombinedIterator<Annotation>(
                annotationIterators);

        Queue<Annotation> prevWords = new ArrayDeque<Annotation>();
        Queue<Annotation> nextWords = new ArrayDeque<Annotation>();

        while (annotIter.hasNext() && nextWords.size() < 10)
            offer(annotIter.next(), nextWords);

        while (!nextWords.isEmpty()) {
            Annotation focus = nextWords.remove();

            if (annotIter.hasNext())
                offer(annotIter.next(), nextWords);
            
            processContext(focus, prevWords, nextWords);

            prevWords.offer(focus);
            if (prevWords.size() > 10)
                prevWords.remove();
        }
    }

    /**
     * Adds the given {@link Annotation} to the end of {@code words} if it's for
     * a noun.
     */
    private static void offer(Annotation annot, Queue<Annotation> words) {
        String pos = AnnotationUtil.pos(annot);
        if (pos == null)
            return;

        if (pos.startsWith("N") ||
            pos.startsWith("V") ||
            pos.startsWith("J") ||
            pos.startsWith("R"))
            words.offer(annot);
    }
}
