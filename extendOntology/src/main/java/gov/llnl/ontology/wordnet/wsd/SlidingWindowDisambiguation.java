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

import com.google.common.collect.Lists;

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
                                           Annotation result,
                                           Queue<Annotation> prevWords,
                                           Queue<Annotation> nextWords);

    /**
     * {@inheritDoc}
     */
    public List<Sentence> disambiguate(List<Sentence> sentences) {
        List<Sentence> results = Lists.newArrayList();

        // Get the sentence iterator.  If no sentences are available, just
        // return.
        Iterator<Sentence> sentIterator = sentences.iterator();
        if (!sentIterator.hasNext())
            return results;

        // Setup the current sentence and the current result sentence that will
        // hold the disambiguated annotations.
        Sentence currSentence = sentIterator.next();
        Sentence currResult = new Sentence(currSentence.start(), 
                currSentence.end(), currSentence.numTokens());
        results.add(currResult);

        // Initalize the annotation iterator and the current annotation index.
        int index = 0;
        Iterator<Annotation> annotIter = currSentence.iterator();

        // Create the annotation queues.  resultWords will contain the
        // annotations that need to be annotated.
        Queue<Annotation> prevWords = new ArrayDeque<Annotation>();
        Queue<Annotation> resultWords = new ArrayDeque<Annotation>();
        Queue<Annotation> nextWords = new ArrayDeque<Annotation>();

        // Fill the next queue with the first 10 nouns found.
        boolean endFound = false;
        while (!endFound && nextWords.size() < 10) {
            // Get the next annotation from the iterator.  If we can, create a
            // new result annotation for it and store it in the same location on
            // the nextWords queue.
            if (annotIter.hasNext()) {
                Annotation word = annotIter.next();

                Annotation result = new Annotation();
                currResult.addAnnotation(index++, result);
                AnnotationUtil.setSpan(result, AnnotationUtil.span(word));

                if (offer(word, nextWords))
                    resultWords.offer(result);
                else
                    continue;
            }
            // If are out of annotations, try to get the next sentence iterator.
            // Create the next result sentence and continue in the loop.
            else if (sentIterator.hasNext()) {
                currSentence = sentIterator.next();
                index = 0;
                currResult = new Sentence(currSentence.start(),
                        currSentence.end(), currSentence.numTokens());
                results.add(currResult);
                annotIter = currSentence.iterator();
                continue;
            }
            // Otherwise we are out of tokens altogether.
            else
                endFound = true;
        }

        // Iterate through each word in the sliding window.  For each focus
        // word, have the subclass disambiguate the focus word using the
        // previous and next contexts.
        while (!nextWords.isEmpty()) {
            // Get the next annotation from the iterator to advance the next
            // window.  If we can, create a new result annotation for it and
            // store it in the same location on the nextWords queue.
            if (annotIter.hasNext()) {
                Annotation word = annotIter.next();

                Annotation result = new Annotation();
                currResult.addAnnotation(index++, result);
                AnnotationUtil.setSpan(result, AnnotationUtil.span(word));

                if (offer(word, nextWords))
                    resultWords.offer(result);
                else
                    continue;
            }
            // If are out of annotations, try to get the next sentence iterator.
            // Create the next result sentence and continue in the loop.
            else if (sentIterator.hasNext()) {
                currSentence = sentIterator.next();
                currResult = new Sentence(currSentence.start(),
                        currSentence.end(), currSentence.numTokens());
                results.add(currResult);
                annotIter = currSentence.iterator();
                continue;
            }
            // If we are out of tokens altogether, that is ok, we will just
            // continue in the loop until nextWords is empty.
            
            // Get the focus word and the corresponding result annotation that
            // will be updated with the sense name.
            Annotation focus = nextWords.remove();
            Annotation result = resultWords.remove();

            // Disambiguate the focus word.
            processContext(focus, result, prevWords, nextWords);

            // Advange the previous window.
            prevWords.offer(focus);
            if (prevWords.size() > 10)
                prevWords.remove();
        }

        return results;
    }

    /**
     * Adds the given {@link Annotation} to the end of {@code words} if it's for
     * a any part of speech represented in wordnet
     */
    private static boolean offer(Annotation annot, Queue<Annotation> words) {
        String pos = AnnotationUtil.pos(annot);
        if (pos == null)
            return false;

        if (pos.startsWith("N") ||
            pos.startsWith("V") ||
            pos.startsWith("J") ||
            pos.startsWith("R")) {
            words.offer(annot);
            return true;
        }
        return false;
    }
}
