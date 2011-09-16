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
import gov.llnl.ontology.wordnet.OntologyReader;
import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;

import com.google.common.collect.Lists;

import edu.stanford.nlp.pipeline.Annotation;

import edu.ucla.sspace.util.CombinedIterator;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;


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
    public Sentence disambiguate(Sentence sentence) {
        return disambiguate(sentence, null);
    }

    /**
     * {@inheritDoc}
     */
    public Sentence disambiguate(Sentence sentence, Set<Integer> focusIndices) {
        Sentence resultSent = new Sentence(
                sentence.start(), sentence.end(), sentence.numTokens());

        // Initalize the annotation iterator and the current annotation index.
        int index = 0;
        Iterator<Annotation> annotIter = sentence.iterator();

        // Create the annotation queues.  resultWords will contain the
        // annotations that need to be annotated.
        Queue<Annotation> prevWords = new ArrayDeque<Annotation>();
        Queue<Annotation> resultWords = new ArrayDeque<Annotation>();
        Queue<Annotation> nextWords = new ArrayDeque<Annotation>();

        // Fill the next queue with the first 10 nouns found.
        while (annotIter.hasNext() && nextWords.size() < 5) {
            // Get the next annotation from the iterator.  If we can, create a
            // new result annotation for it and store it in the same location on
            // the nextWords queue.
            Annotation word = annotIter.next();
            Annotation result = new Annotation();
            resultSent.addAnnotation(index++, result);
            AnnotationUtil.setSpan(result, AnnotationUtil.span(word));
            nextWords.offer(word);
            resultWords.offer(result);
        }

        // Iterate through each word in the sliding window.  For each focus
        // word, have the subclass disambiguate the focus word using the
        // previous and next contexts.
        int focusIndex = 0;
        while (!nextWords.isEmpty()) {
            // Get the next annotation from the iterator to advance the next
            // window.  If we can, create a new result annotation for it and
            // store it in the same location on the nextWords queue.
            if (annotIter.hasNext()) {
                Annotation word = annotIter.next();

                Annotation result = new Annotation();
                resultSent.addAnnotation(index++, result);
                AnnotationUtil.setSpan(result, AnnotationUtil.span(word));

                nextWords.offer(word);
                resultWords.offer(result);
            }

            // Get the focus word and the corresponding result annotation that
            // will be updated with the sense name.
            Annotation focus = nextWords.remove();
            Annotation result = resultWords.remove();

            // Disambiguate the focus word if it's in the selected list.
            if (focusIndices == null ||
                focusIndices.isEmpty() ||
                focusIndices.contains(focusIndex++))
                processContext(focus, result, prevWords, nextWords);

            // Advange the previous window.
            prevWords.offer(focus);
            if (prevWords.size() > 5)
                prevWords.remove();
        }

        return resultSent;
    }

    /**
     * Adds the given {@link Annotation} to the end of {@code words} if it's for
     * a any part of speech represented in wordnet
     */
    private static boolean offer(Annotation annot, Queue<Annotation> words) {
        String pos = AnnotationUtil.pos(annot);
        if (pos == null ||
            pos.startsWith("N") ||
            pos.startsWith("V") ||
            pos.startsWith("J") ||
            pos.startsWith("R")) {
            words.offer(annot);
            return true;
        }
        return false;
    }

    /**
     * Returns all of the {@link Synset}s found given the word and part of
     * speech information, if present, in {@code annot}.  If the part of speech
     * is available, but provides no synsets, all possible synsets are returned
     * for the word, under the assumption that the tag may be incorrect.
     */
    protected Synset[] getSynsets(OntologyReader reader, Annotation annot) {
        String word = AnnotationUtil.word(annot);
        String pos = AnnotationUtil.pos(annot);
        if (pos == null) 
            return reader.getSynsets(word);

        Synset[] synsets = reader.getSynsets(
                word, PartsOfSpeech.fromPennTag(pos));
        if (synsets == null || synsets.length == 0)
            return reader.getSynsets(word);
        return synsets;
    }
}
