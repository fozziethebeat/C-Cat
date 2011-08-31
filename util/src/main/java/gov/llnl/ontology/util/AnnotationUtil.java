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

import edu.stanford.nlp.ling.CoreAnnotations.CoNLLDepTypeAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CoNLLDepParentIndexAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SpanAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.WordSenseAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.IntPair;


/**
 * A simple utility class for accessing the most frequent {@link Annotation}s
 * applied to a {@link Annotation}.  
 *
 * @author Keith Stevens
 */
public class AnnotationUtil {

    /**
     * Returns the string form of the part of speech tag for {@code annot}.
     */
    public static String pos(Annotation annot) {
        return annot.get(PartOfSpeechAnnotation.class);
    }

    /**
     * Sets the part of speech tag for {@code annot}.
     */
    public static void setPos(Annotation annot, String pos) {
        annot.set(PartOfSpeechAnnotation.class, pos);
    }

    /**
     * Returns the token for {@code annot}.
     */
    public static String word(Annotation annot) {
        return annot.get(TextAnnotation.class);
    }

    /**
     * Sets the token for {@code annot}
     */
    public static void setWord(Annotation annot, String word) {
        annot.set(TextAnnotation.class, word);
    }

    /**
     * Returns the word sense for {@code annot}.
     */
    public static String wordSense(Annotation annot) {
        return annot.get(WordSenseAnnotation.class);
    }

    /**
     * Sets the word sense for {@code annot}.
     */
    public static void setWordSense(Annotation annot, String sense) {
        annot.set(WordSenseAnnotation.class, sense);
    }

    /**
     * Returns the index span for {@link annot}
     */
    public static IntPair span(Annotation annot) {
        return annot.get(SpanAnnotation.class);
    }

    /**
     * Sets the index space for {@code annot}.
     */
    public static void setSpan(Annotation annot, int start, int end) {
        setSpan(annot, new IntPair(start, end));
    }

    /**
     * Sets the index space for {@code annot}.
     */
    public static void setSpan(Annotation annot, IntPair span) {
        annot.set(SpanAnnotation.class, span);
    }

    /**
     * Returns the index of the governing mode in a dependency parse tree for
     * {@code annot}.
     */
    public static int dependencyParent(Annotation annot) {
        if (annot.has(CoNLLDepParentIndexAnnotation.class))
            return annot.get(CoNLLDepParentIndexAnnotation.class);
        return -1;
    }

    /**
     * Sets the index of the governing mode in a dependency parse tree for
     * {@code annot}.
     */
    public static void setDependencyParent(Annotation annot, int index) {
        annot.set(CoNLLDepParentIndexAnnotation.class, index);
    }

    /**
     * Returns the relation between {@code annot} and the governing mode in a
     * dependency parse tree for {@code annot}.
     */
    public static String dependencyRelation(Annotation annot) {
        return annot.get(CoNLLDepTypeAnnotation.class);
    }

    /**
     * Sets the relation between {@code annot} and the governing mode in a
     * dependency parse tree for {@code annot}.
     */
    public static void setDependencyRelation(Annotation annot, String relation) {
        annot.set(CoNLLDepTypeAnnotation.class, relation);
    }
}

