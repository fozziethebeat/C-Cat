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

import edu.berkeley.nlp.PCFGLA.CoarseToFineMaxRuleParser;
import edu.berkeley.nlp.PCFGLA.Grammar;
import edu.berkeley.nlp.PCFGLA.Lexicon;
import edu.berkeley.nlp.PCFGLA.ParserData;
import edu.berkeley.nlp.PCFGLA.TreeAnnotations;

import edu.berkeley.nlp.ling.Tree;

import edu.berkeley.nlp.util.Numberer;

import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;

import reconcile.featureExtractor.ParserStanfordParser;

import reconcile.general.BerkeleyToStanfordTreeConverter;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOError;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * A {@link Parser} wrapper for the Berkeley parser.
 *
 * </p>
 *
 * NOTE: This should be moved to Trinidad.
 *
 * @author Keith Stevens
 */
public class BerkeleyParser implements Parser {
  
  /**
   * The raw parser.
   */
  private CoarseToFineMaxRuleParser parser;

  /**
   * A factory that helps transform stanford based phrase structure trees into
   * dependency trees.
   */
  private GrammaticalStructureFactory gsf;

  /**
   * Creates a new {@link BerkeleyParser}.  The data model specified by {@code
   * parserPath} is assumed to reside in the current running jar.  If running
   * within a map reduce job, this is the easiset constructor to use.
   */
  public BerkeleyParser(String parserPath) {
    this(parserPath, true);
  }

  /**
   * Creates a new {@link BerkeleyParser}.  If {@code fromJar} is true, the data
   * model specified by {@code parserPath} is assumed to reside in the current
   * running jar, otherwise it is assumed to be a real path.  If not running in
   * a map reduce job, this is likely the constructor to use.
   */
  public BerkeleyParser(String parserPath, boolean fromJar) {
    try {
      // Create the input stream.
      InputStream parserStream = (fromJar) 
        ? this.getClass().getResourceAsStream(parserPath)
        : new FileInputStream(parserPath);

      // Read all of the required data models for the parser.
      ParserData pData = ParserData.Load(parserStream);
      Grammar grammar = pData.getGrammar();
      Lexicon lexicon = pData.getLexicon();
      Numberer.setNumberers(pData.getNumbs());

      // Set some paramaters for the parser.
      double threshold = 1.0;
      boolean viterbiInsteadOfMaxRule = false;
      boolean outputSubCategories = false;
      boolean outputInsideScoresOnly = false;
      boolean accuracyOverEfficiency = false;

      // Create the parser.
      parser = new CoarseToFineMaxRuleParser(
          grammar, lexicon, threshold, -1, viterbiInsteadOfMaxRule,
          outputSubCategories, outputInsideScoresOnly, accuracyOverEfficiency, 
          false, false);

      // Some additional components needed for creating the dependency parse
      // conversions
      PennTreebankLanguagePack tlp = new PennTreebankLanguagePack();
      gsf = tlp.grammaticalStructureFactory();
    } catch (IOException ioe) {
      throw new IOError(ioe);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void parseSentenceToSpans(String text,
                                   Annotation sentence,
                                   AnnotationSet tokSet,
                                   AnnotationSet posSet,
                                   AnnotationSet parses,
                                   AnnotationSet depAnnots) {
    // Constrain the token and part of speech sets to the words within the
    // sentence.
    tokSet = tokSet.getContained(sentence);
    posSet = posSet.getContained(sentence);

    // Create the token list that represents the sentence.
    List<String> tokList = new ArrayList();
    Iterator<Annotation> tokenIter = tokSet.iterator();
    while (tokenIter.hasNext()) {
      Annotation tok = (tokenIter.next());
      tokList.add(text.substring(tok.getStartOffset(), tok.getEndOffset()));
    }
    
    // Avoid parsing any sentence that is empty.  NOTE: empty sentences cause
    // the parser to throw an array out of bounds exception.
    if (tokList.size() == 0)
      return;

    // Parse the sentence and add the phrase structure parses to the annotation
    // set.
    Tree<String> parseTree = parser.getBestConstrainedParse(tokList, null);
    parseTree = TreeAnnotations.unAnnotateTree(parseTree);
    addSpans(parseTree, 0, tokSet.toArray(), parses, Annotation.getNullAnnot());

    // Convert the phrase structure tree to a dependency parse tree and store
    // the tree to the annotation set.
    GrammaticalStructure gs = gsf.newGrammaticalStructure(
        BerkeleyToStanfordTreeConverter.convert(parseTree));
    Collection<TypedDependency> dep = gs.typedDependencies();
    ParserStanfordParser.addDepSpans(dep, tokSet.toArray(), depAnnots);
  }

  /**
   * Recursively adds {@link Annotation}s for the phrase structure tree to
   * {@code parsesSet}.
   *
   * @param parseTree The Berkeley based phrase structure parse tree
   * @param startTokenIndx The index of the first token that still needs to be
   *        added
   * @param sentToks An array of token {@link Annotation}s for the current
   *        sentence
   * @param parsesSet The {@link AnnotationSet} to which tree nodes are stored
   * @param parent The parent {@link Annotation} node of the current tree {@link
   *        Annotation} node being written
   */
  private int addSpans(Tree<String> parseTree, 
                       int startTokenIndx, 
                       Annotation[] sentToks, 
                       AnnotationSet parsesSet,
                       Annotation parent) {
    int yieldLength = parseTree.getYield().size();
    Map<String, String> attrs = new TreeMap<String, String>();

    attrs.put("parent", Integer.toString(parent.getId()));

    int curId = parsesSet.add(
        sentToks[startTokenIndx].getStartOffset(),
        sentToks[yieldLength + startTokenIndx - 1].getEndOffset(), 
        parseTree.getLabel(), attrs);

    Annotation cur = parsesSet.get(curId);
    addChild(parent, cur);
    int offset = startTokenIndx;

    for (Tree<String> tr : parseTree.getChildren())
      if (!tr.isLeaf())
        offset += addSpans(tr, offset, sentToks, parsesSet, cur);

    return yieldLength;
  }

  /**
   * sets a child attribute between a {@code parent} and {@code child} parse
   * nodes.
   *
   * </p>
   * From Reconcile.
   */
  private static void addChild(Annotation parent, Annotation child) {
    if (!parent.equals(Annotation.getNullAnnot())) {
      String childIds = parent.getAttribute("CHILD_IDS");
      childIds = (childIds == null) 
        ? Integer.toString(child.getId())
        : childIds + "," + child.getId();
      parent.setAttribute("CHILD_IDS", childIds);
    }
  }
}

