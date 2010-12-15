/*
 * Copyright (c) 2010, Lawrence Livermore National Security, LLC. Produced at
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

package gov.llnl.ontology.mapreduce;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;


/**
 * An interface the wraps a parser such that it correctly and safely parses a
 * sentence specified by an {@link Annotation} and stores the phrase structure
 * tree and dependency parse tree in separate {@link AnnotationSet}s.
 *
 * </p>
 *
 * If implementations require a data model to be loaded from disk, it is
 * suggested that two constructors are provided: one that automatically reads
 * the data model from a file within the current running jar and one that lets 
 * the user specify whether or not the a file path is within a jar.
 *
 * </p>
 *
 * Note: This should be moved to trinidad.
 *
 * @author Keith Stevens
 */
public interface Parser {

  /**
   * Parses the {@code sentence} embedded within {@code text}.  The phrase
   * structure parse tree will be stored in {@code parses} and the dependency
   * parse tree will be stored in {@code dpeParses}.
   *
   * @param text The complete document text
   * @param sentence An {@link Annotation} that specifies the set of characters
   *        that compose a sentence
   * @param tokenSet An {@link AnnotationSet} that contains token annotations
   *        for at least all of the words specified by {@code sentence}
   * @param posSet An {@link AnnotationSet} that contains part of speech 
   *        annotations for at least all of the words specified by {@code
   *        sentence}
   * @param parses The {@link AnnotationSet} to which phrase structure tree
   *        annotations will be written
   * @param depParses The {@link AnnotationSet} to which dependency parse tree
   *        annotations will be written
   */
  void parseSentenceToSpans(String text,
                            Annotation sentence,
                            AnnotationSet tokenSet,
                            AnnotationSet posSet,
                            AnnotationSet parses,
                            AnnotationSet depParses);
}
