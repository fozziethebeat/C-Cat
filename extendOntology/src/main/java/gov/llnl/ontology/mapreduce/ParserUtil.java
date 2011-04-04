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

import edu.stanford.nlp.trees.Tree;

import org.apache.hadoop.hbase.client.Result;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;

import reconcile.general.Constants;

import trinidad.hbase.table.DocSchema;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is a utility class that does sentence splitting on {@link
 * AnnotationSet}s prior to parsing each sentence with an actual {@link Parser}.
 * This class wraps {@link Parser} instances.
 *
 * </p>
 *
 * This class should be moved to trinidad and folded into a parser base class.
 *
 * @author Keith Stevens
 */
public class ParserUtil {

  /**
   * The maximum number of tokens permitted in a single sentence.
   */
  private static final int TOKEN_LIMIT = 100;

  /**
   * Characters which designate when a sentence should be split into smaller
   * sentences.
   */
  private static final String[] BREAKS = { ";", ":", ",", "." };


  /**
   * The {@link Parser} used to process each sentence.
   */
  public Parser parser;

  /**
   * Creates a new {@link ParserUtil} instance.
   */
  public ParserUtil(Parser parser) {
    this.parser = parser;
  }

  /**
   * Returns true if the sentences in {@code text} were successfully parsed.  If
   * they were parsed, a phrase structure tree will be serialized and stored in
   * {@code parses} and a dependency structure tree will be serialized and
   * stored in {@code depAnnots}.
   */
  public boolean parseFromDocSchema(String text,
                                    Result row, 
                                    AnnotationSet parses,
                                    AnnotationSet depAnnots,
                                    AnnotationSet posSet) {
    // Get the required annotation sets.
    AnnotationSet tokSet = DocSchema.getAnnotationSet(
        row, Constants.TOKEN);
    AnnotationSet sentSet = DocSchema.getAnnotationSet(
        row, Constants.SENT);

    // Skip any rows that lack the basic annotations.
    if (tokSet == null || sentSet == null || posSet == null)
      return false;

    // Parse all of the sentences.
    if (!parse(text, sentSet, tokSet, posSet, parses, depAnnots))
      return false;

    // The sentences were parsed successfully.
    return true;
  }

  /**
   * Returns the substring of {@code text} the corresponds to the offsets
   * specified by {@code annot}.
   */
  public static String getAnnotText(Annotation annot, String text) {
    return text.substring(annot.getStartOffset(), annot.getEndOffset());
  }

  /**
   * Parses the sentences referred to by {@link sentSet} and adds {@link
   * Annotation}s for phrase structure parses and dependency trees to {@code
   * parses} and {@code depAnnots} respectively.
   */
  public boolean parse(String text, AnnotationSet sentSet, 
                       AnnotationSet tokSet, AnnotationSet posSet,
                       AnnotationSet parses, AnnotationSet depAnnots) {
    // loop through sentences
    for (Annotation sentence : sentSet) {
      // The parser doesn't handle well sentences that are all caps.  Need
      // to find those cases
      String sentText = getAnnotText(sentence, text);

      // Split the sentence if it really contains more segments.
      AnnotationSet sentenceTok = tokSet.getContained(sentence);
      List<Annotation> splitSent = splitSentence(
          sentenceTok, sentence, text);

      // Parse each segment and add the parse tree information for the
      // phrase structure tree and the dependency parse tree to the
      // annotation sets.
      for (Annotation sent : splitSent)
        parser.parseSentenceToSpans(
            text, sent, tokSet, posSet, parses, depAnnots);
    }
    return true;
  }

  /**
   * Splits a sentence up into smaller annotations.
   */
  private static List<Annotation> splitSentence(AnnotationSet toks, 
                                                Annotation sentence,
                                                String text) {
    List<Annotation> result = new ArrayList<Annotation>();
    // If the sentence is already below the size limit, accept it as is.
    if (toks.size() <= TOKEN_LIMIT) {
      result.add(sentence);
      return result;
    }

    // Guess the break point for the sentence.
    List<Annotation> tokens = toks.getOrderedAnnots();
    int mid = tokens.size() / 2;

    // For each possible separation character, pass through the sentence and
    // find any occurrences of the separation character.  If found, split the
    // sentence.
    for (String br : BREAKS) {
      for (int indexLow = mid, indexHigh = mid;
           indexLow >= 0 && indexHigh < tokens.size(); indexLow--) {
        indexHigh++;

        // Try to split in the lower half of the text.
        Annotation tokLow = tokens.get(indexLow);
        if (br.equals(getAnnotText(tokLow, text)))
          return addSplit(text, sentence, toks, tokLow, result);

        // Try to split in the upper half of the text.
        Annotation tokHigh = tokens.get(indexHigh);
        if (br.equals(getAnnotText(tokHigh, text)))
          return addSplit(text, sentence, toks, tokHigh, result);
      }
    }

    result.add(sentence);
    return result;
  }

  private static List<Annotation> addSplit(String text, 
                                           Annotation sentence,
                                           AnnotationSet toks,
                                           Annotation splitTok,
                                           List<Annotation> result) {
    Annotation newSen = new Annotation(
        sentence.getId(), sentence.getStartOffset(), 
        splitTok.getEndOffset(), "sentence");
    result = splitSentence(toks.getContained(newSen), newSen, text);
    Annotation newSen1 = new Annotation(
        sentence.getId(), splitTok.getEndOffset() + 1,
        sentence.getEndOffset(), "sentence");
    result.addAll(splitSentence(
          toks.getContained(newSen1), newSen1, text));
    return result;
  }
}
