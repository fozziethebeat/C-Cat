/*
 * Copyright (c) 2010, Lawrence Livermore National Security, LLC. Produced at
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

package gov.llnl.ontology.wordnet;

import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;
import gov.llnl.ontology.wordnet.Synset.Relation;
import gov.llnl.ontology.wordnet.Synset.Relation.*;

import java.util.List;
import java.util.Map;


/**
 * This is a collection of WordNet similarity measures.  Sevearl of these are
 * the same measures provided by the perl <a
 * href="http://wn-similarity.sourceforge.net/">WordNet::Similarity</a> package
 * distributed by Ted Pedersen.
 *
 * </p>
 *
 * The similarity measures come in three varieties: measures based on the paths
 * between two synsets, measures based on information content for each synset,
 * and a combination of path information and information content.
 *
 * @author Keith Stevens
 */
public class Similarity {

  /**
   * Returns a score measuring how similar two {@link Synset}s are based on the
   * shortest path that connects them when using only parent and child links.
   * Scores range from 0 to 1, with 0 being returned for {@link Synset}s that
   * have no connecting path.
   */
  public static double path(Synset synset1, Synset synset2) {
    int distance = SynsetRelations.shortestPathDistance(synset1, synset2);
    return (distance >= 0)
      ? 1d / (distance + 1)
      : 0;
  }

  /**
   * Returns the Leacock Chodorow Similarity measure.  This measure scores
   * {@link Synset} similarity based on the shortest path connecting the two and
   * the maximum depth of the taxonomy for the particular part of speech.  The
   * score is given as -log(p/2d) where p is the shortest path length and d is
   * the maximum taxonomy depth.  If {@code synset1} and {@code synset2} have
   * different {@link PartsOfSpeech}, -1 is returned.
   */
  public static double lch(Synset synset1, Synset synset2) {
    if (synset1.getPartOfSpeech() != synset2.getPartOfSpeech())
      return 0;
    WordNetCorpusReader wordnet = WordNetCorpusReader.getWordNet();
    int maxDepth = wordnet.getMaxDepth(synset1.getPartOfSpeech());
    int distance = SynsetRelations.shortestPathDistance(synset1, synset2);
    return (distance >= 0 && distance <= Integer.MAX_VALUE)
      ? -1 * Math.log((distance + 1) / (2d * maxDepth))
      : 0;
  }

  /**
   * Returns a scaled version of the Leacock Chodorow Similarity measure.  This
   * is guaranateed to return a value between 0 and 1.  The normalization factor
   * is unique for each part of speech and WordNet version.  The scaling is done
   * by dividing the raw lch similarity by the maximum similarity for a
   * particular part of speech.  This maximum similarity is defined by the depth
   * of the hierarchy for that part of speech.
   */
  public static double lchScaled(Synset synset1, Synset synset2) {
    WordNetCorpusReader wordnet = WordNetCorpusReader.getWordNet();
    double lchSim = lch(synset1, synset2);
    int maxDepth = wordnet.getMaxDepth(synset1.getPartOfSpeech());
    double maxSim = -1 * Math.log(1/(2d* maxDepth));
    return lchSim / maxSim;
  }

  /**
   * Returns the Wu-Palmer Similarity.  This measure scores {@link Synset}s
   * based on the depth of the two {@link Synset}s in the taxonomy and their
   * lowest common subsumer, i.e. the deepest node in the tree that is a
   * hypernym of both {@link Synset}s.  This lowest common subsumer is not
   * always the same as the shared hypernym that forms the shortest path between
   * the two {@link Synset}s. When there are multiple subsumers, the subsumer
   * with the longest path to the root of the taxonomy is selected.
   */
   public static double wup(Synset synset1, Synset synset2) {
    Synset subsumer = SynsetRelations.lowestCommonHypernym(synset1, synset2);
    if (subsumer == null)
      return 0;
    double depth = subsumer.getMaxDepth() + 1;
    if (subsumer.getPartOfSpeech() == PartsOfSpeech.NOUN)
      depth++;
    double distance1 = SynsetRelations.shortestPathDistance(synset1, subsumer);
    double distance2 = SynsetRelations.shortestPathDistance(synset2, subsumer);
    distance1 += depth;
    distance2 += depth;
    return (2.0 * depth) / (distance1 + distance2);
  }

  /**
   * Returns the Resnick similarity.  This measure scores {@link Synset}s with
   * the information content of their lowest common subsumer.
   */
  public static double res(Synset synset1,
                           Synset synset2,
                           InformationContent ic) {
    List<Synset> subsumers = SynsetRelations.lowestCommonHypernyms(
        synset1, synset2);
    if (subsumers.size() == 0)
      return 0;
    double bestIC = 0;
    for (Synset subsumer : subsumers)
      bestIC = Math.max(bestIC, ic.informationContent(subsumer));
    return bestIC;
  }

  /**
   * Returns the Jiang-Conrath Similarity.  This measure scores {@link Synset}s
   * based on the information content of their lowest common subsumer and the
   * information content of the two {@link Synset}s.  Formally, this is 1 /
   * (IC({@code synset1}) + IC({@code synset2}) - 2 * IC({@code lcs}))
   */
  public static double jcn(Synset synset1,
                           Synset synset2,
                           InformationContent ic) {
    if (synset1.equals(synset2))
      return Double.MAX_VALUE;
    double ic1 = ic.informationContent(synset1);
    double ic2 = ic.informationContent(synset2);
    if (ic1 == -1 || ic2 == -1)
      return 0;
    double icSubsumer = res(synset1, synset2, ic);
    double  difference = ic1 + ic2 - 2 * icSubsumer;
    return (difference == 0) ? Double.MAX_VALUE : 1d / difference;
  }

  /**
   * Returns the Lin Similarity.  This measure scores {@link Synset}s based on
   * the information content of their lowest common subsumer and of the two
   * {@link Synset}s.  Formally, this is:
   * 2 * IC({@code lcs}) / (IC({@code * synset1}) + IC({@code synset2}))
   */
  public static double lin(Synset synset1,
                           Synset synset2,
                           InformationContent ic) {
    double ic1 = ic.informationContent(synset1);
    double ic2 = ic.informationContent(synset2);
    if (ic1 == -1 || ic2 == -1)
      return 0;
    double icSubsumer = res(synset1, synset2, ic);
    return (2d * icSubsumer) / (ic1 + ic2);
  }

  /**
   * Returns a word net path similarity measure defined by Hirst and St-Onge.
   * This measure 8 types of valid paths between synsets.  The similarity is
   * measured based on the number of links used within a valid path and the
   * number of times the path changes direction.  Path directions are broken
   * into three categories: upward, which includes any generalization
   * relation; downward, which includes any specification relation;
   * and side, which includes all other relations.  For example, Hypernymy is
   * has an upward direction, Hyponymy has a downward direction, and Similar Too
   * has a side direction.  A path may repeat the previous direction as many
   * times as needed and still be considered valid.  Invalid paths are not used
   * to compute the similarity.  Any path can have a maximum of 5 links.
   *
   * </p>
   * Formally, the similarity is:
   * </br>
   *   C - pathLength - k * |direction changes|
   * Since C and k are both constants, they are set to 1.
   */
  public static double hso(Synset synset1, Synset synset2) {
    double bestScore = 0;
    for (Relation relation : UP_RELATION)
      for (Synset related : synset1.getRelations(relation))
        bestScore = Math.max(bestScore, hsoState1(1, 0, related, synset2));

    for (Relation relation : DOWN_RELATION)
      for (Synset related : synset1.getRelations(relation))
        bestScore = Math.max(bestScore, hsoState3(1, 0, related, synset2));

    for (Relation relation : SIDE_RELATION)
      for (Synset related : synset1.getRelations(relation))
        bestScore = Math.max(bestScore, hsoState2(1, 0, related, synset2));

    // With k == 1 the minimum score possible is 0 and the maximum
    // score is C.  Divide by C so that it's within a range of 0 to 1.
    return bestScore / C;
  }

  /**
   * The fixed value for C when computing the best score.
   */
  private static final double C = 8;

  /**
   * The set of relations with an upward direction.
   */
  private static Relation[] UP_RELATION = {
    Relation.HYPERNYM, Relation.MEMBER_MERONYM,
    Relation.SUBSTANCE_MERONYM, Relation.PART_MERONYM
  };

  /**
   * The set of relations with a downward direction.
   */
  private static Relation[] DOWN_RELATION = {
    Relation.HYPONYM, Relation.MEMBER_HOLONYM, Relation.SUBSTANCE_HOLONYM, 
    Relation.PART_HOLONYM, Relation.CAUSE, Relation.ENTAILMENT
  };

  /**
   * The set of relations with a side direction.
   */
  private static Relation[] SIDE_RELATION = {
    Relation.ALSO_SEE, Relation.ATTRIBUTE, 
    Relation.ANTONYM, Relation.SIMILAR_TO
  };

  /**
   * Traverses valid outward links from {@code src} node.  If {@code src} equals
   * {@code dest}, then a score is computed.  Of all possible paths from this
   * state, the maximum score is returned.
   *
   * </p>
   *
   * From this state, all directions are valid.
   */
  private static double hsoState1(int depth, int dirChange,
                                  Synset src, Synset dest) {
    double bestScore = (src.equals(dest)) ? C - depth - dirChange : 0;
    depth++;

    if (depth > 5)
      return Math.max(bestScore, 0);

    for (Relation relation : UP_RELATION)
      for (Synset related : src.getRelations(relation))
        bestScore = Math.max(
            bestScore, hsoState1(depth, dirChange, related, dest));

    dirChange++;
    for (Relation relation : DOWN_RELATION)
      for (Synset related : src.getRelations(relation))
        bestScore = Math.max(
            bestScore, hsoState4(depth, dirChange, related, dest));

    for (Relation relation : SIDE_RELATION)
      for (Synset related : src.getRelations(relation))
        bestScore = Math.max(
            bestScore, hsoState2(depth, dirChange, related, dest));

    return bestScore;
  }

  /**
   * Traverses valid outward links from {@code src} node.  If {@code src} equals
   * {@code dest}, then a score is computed.  Of all possible paths from this
   * state, the maximum score is returned.
   *
   * </p>
   *
   * From this state, only side and downward directions are valid.
   */
  private static double hsoState2(int depth, int dirChange, 
                                  Synset src, Synset dest) {
    double bestScore = (src.equals(dest)) ? C - depth - dirChange : 0;
    depth++;

    if (depth > 5)
      return Math.max(bestScore, 0);

    for (Relation relation : SIDE_RELATION)
      for (Synset related : src.getRelations(relation))
        bestScore = Math.max(
            bestScore, hsoState2(depth, dirChange, related, dest));

    dirChange++;
    for (Relation relation : DOWN_RELATION)
      for (Synset related : src.getRelations(relation))
        bestScore = Math.max(
            bestScore, hsoState4(depth, dirChange, related, dest));


    return bestScore;
  }

  /**
   * Traverses valid outward links from {@code src} node.  If {@code src} equals
   * {@code dest}, then a score is computed.  Of all possible paths from this
   * state, the maximum score is returned.
   *
   * </p>
   *
   * From this state, only side and downward directions are valid.
   */
  private static double hsoState3(int depth, int dirChange, 
                                  Synset src, Synset dest) {
    double bestScore = (src.equals(dest)) ? C - depth - dirChange : 0;
    depth++;

    if (depth > 5)
      return Math.max(bestScore, 0);

    for (Relation relation : DOWN_RELATION)
      for (Synset related : src.getRelations(relation))
        bestScore = Math.max(
            bestScore, hsoState4(depth, dirChange, related, dest));

    dirChange++;
    for (Relation relation : SIDE_RELATION)
      for (Synset related : src.getRelations(relation))
        bestScore = Math.max(
            bestScore, hsoState5(depth, dirChange, related, dest));

    return bestScore;
  }

  /**
   * Traverses valid outward links from {@code src} node.  If {@code src} equals
   * {@code dest}, then a score is computed.  Of all possible paths from this
   * state, the maximum score is returned.
   *
   * </p>
   *
   * From this state, only downward directions are valid.
   */
  private static double hsoState4(int depth, int dirChange, 
                                  Synset src, Synset dest) {
    double bestScore = (src.equals(dest)) ? C - depth - dirChange : 0;
    depth++;

    if (depth > 5)
      return Math.max(bestScore, 0);

    for (Relation relation : DOWN_RELATION)
      for (Synset related : src.getRelations(relation))
        bestScore = Math.max(
            bestScore, hsoState4(depth, dirChange, related, dest));

    return bestScore;
  }

  /**
   * Traverses valid outward links from {@code src} node.  If {@code src} equals
   * {@code dest}, then a score is computed.  Of all possible paths from this
   * state, the maximum score is returned.
   *
   * </p>
   *
   * From this state, only side directions are valid.
   */
  private static double hsoState5(int depth, int dirChange, 
                                  Synset src, Synset dest) {
    double bestScore = (src.equals(dest)) ? C - depth - dirChange : 0;
    depth++;

    if (depth > 5)
      return Math.max(bestScore, 0);

    for (Relation relation : SIDE_RELATION)
      for (Synset related : src.getRelations(relation))
        bestScore = Math.max(
            bestScore, hsoState5(depth, dirChange, related, dest));

    return bestScore;
  }
}
