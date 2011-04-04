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

import edu.stanford.nlp.ling.TaggedWord;

import edu.stanford.nlp.parser.lexparser.LexicalizedParser;

import edu.stanford.nlp.trees.EnglishGrammaticalRelations;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;

import reconcile.featureExtractor.ParserStanfordParser;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * A {@link Parser} wrapper for the Stanford parser.
 *
 * @author Keith Stevens
 */
public class StanfordParser implements Parser{

  /**
   * The raw parser.
   */
  private LexicalizedParser parser;

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
  public StanfordParser(String parserPath) {
    this(parserPath, true);
  }

  /**
   * Creates a new {@link BerkeleyParser}.  If {@code fromJar} is true, the data
   * model specified by {@code parserPath} is assumed to reside in the current
   * running jar, otherwise it is assumed to be a real path.  If not running in
   * a map reduce job, this is likely the constructor to use.
   */
  public StanfordParser(String parserPath, boolean fromJar) {
    try {
      // Create the input stream.
      InputStream inStream = (fromJar)
        ? this.getClass().getResourceAsStream(parserPath)
        : new FileInputStream(parserPath);

      // Create the parser.
      parser = new LexicalizedParser(new ObjectInputStream(inStream));

      // Create the factory used to create the dependency parse trees.
      gsf = parser.getOp().tlpParams.treebankLanguagePack().grammaticalStructureFactory();
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
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
    // Limit the tokens that we will inspect to be those within the bounds of
    // the requested sentence.
    tokSet = tokSet.getContained(sentence);
    posSet = posSet.getContained(sentence);

    // Parse.
    Tree parseTree = parseSentence(text, tokSet, posSet);

    // Add the parse tree information.
    addSpans(parseTree, 0, tokSet.toArray(), parses, Annotation.getNullAnnot());
    GrammaticalStructure gs = gsf.newGrammaticalStructure(parseTree);
    ParserStanfordParser.addDepSpans(
        gs.typedDependencies(), tokSet.toArray(), depAnnots);
  }

  /**
   * Parses an complete sentence specified by {@code tokenSet} and {@code
   * posSet}.  The call is synchronized because the parser itself is not thread
   * safe.
   */
  private synchronized Tree parseSentence(String text,
                                          AnnotationSet tokenSet,
                                          AnnotationSet posSet) {
    List<TaggedWord> tokenList = new ArrayList<TaggedWord>();

    // Build up the sentence as a list of tokens, each of which will have a
    // word  and it's parts of speech.
    Iterator<Annotation> tokenIter = tokenSet.iterator();
    Iterator<Annotation> posIter = posSet.iterator();
    List<String> tokList = new ArrayList<String>();
    while (tokenIter.hasNext() && posIter.hasNext()) {
      Annotation tok = tokenIter.next();
      String token = text.substring(tok.getStartOffset(), tok.getEndOffset());

      String pos = posIter.next().getType();
      tokenList.add(new TaggedWord(token, pos));
      tokList.add(token);
    }

    // Cannot parse a zero length sentence.
    if (tokenList.size() == 0)
      return null;

    // Parse.
    parser.parse(tokenList);
    return parser.getBestParse();
  }

  /**
   * Recursively adds {@link Annotation}s for the phrase structure tree to
   * {@code parsesSet}.
   *
   * </p>
   * From Reconcile.
   *
   * @param parseTree The Stanford based phrase structure parse tree
   * @param startTokenIndx The index of the first token that still needs to be
   *        added
   * @param sentToks An array of token {@link Annotation}s for the current
   *        sentence
   * @param parsesSet The {@link AnnotationSet} to which tree nodes are stored
   * @param parent The parent {@link Annotation} node of the current tree {@link
   *        Annotation} node being written
   */
  @SuppressWarnings("unchecked")
  private int addSpans(Tree parseTree,
                       int startTokenIndx,
                       Annotation[] sentToks,
                       AnnotationSet parsesSet,
                       Annotation parent) {
    // Adding parse tree annotations to parsesSet.
    List yield = parseTree.yield();
    int len = yield.size();

    // Add in the parent atttribute for the annotation.
    Map<String, String> attrs = new TreeMap<String, String>();
    attrs.put("parent", Integer.toString(parent.getId()));

    // Add the annotation for the parent.
    int cur = parsesSet.add(sentToks[startTokenIndx].getStartOffset(),
                            sentToks[len + startTokenIndx - 1].getEndOffset(),
                            parseTree.value(), attrs);

    Annotation curAn = parsesSet.get(cur);
    addChild(parent, curAn);

    // Recursively add annotations for the children nodes.
    int offset = startTokenIndx;
    for (Tree tr : parseTree.children())
      offset += addSpans(tr, offset, sentToks, parsesSet, curAn);
    return len;
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
