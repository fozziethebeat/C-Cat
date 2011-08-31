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

import edu.ucla.sspace.util.Duple;
import edu.ucla.sspace.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * This represents a collection of methods that determine the relationship
 * between two {@link Synset}s.  These relations include existence of a parent
 * relationship between two {@link Synset}s, existence of a cousin relationship
 * between two {@link Synest}s, and methods for finding lowest common parents of
 * two {@link Synset}s.
 *
 * @author Keith Stevens
 */
public class SynsetRelations {

    /**
     * The possible hypernym statuses.
     */
    public enum HypernymStatus {
        TERMS_MISSING,
        NOVEL_HYPONYM,
        NOVEL_HYPERNYM,
        KNOWN_HYPERNYM,
        KNOWN_NON_HYPERNYM,
    }


    /**
     * Returns the hypernym relationship between some {@link Synset} of {@code
     * childTerm} and some {@link Synset} of {@code ancestorTerm}.  If neither
     * term is found in wordnet, {@link HypernymStatus#TERMS_MISSING} is
     * returned.  If {@code childTerm} has a mapping but {@code ancestorTerm} is
     * missing from wordnet, {@link HypernymStatus#NOVEL_HYPERNYM} is returned.
     * If vice-versa, {@link HyernymStatus#NOVEL_HYPONYM}.  If both terms have a
     * mapping and some {@link Synset} of {@code ancestorTerm} acts as an
     * ancestors of some {@link Synset} of {@code childTerm}, this returns
     * {@link HypernymStatus#KNOWN_HYPERNYM}.  If none of the above, this
     * returns {@link HypernymStatus#KNOWN_NON_HYPERNYM}.
     *
     * @param childTerm A term which might have a {@link Synset} that is a child
     *        of a {@link Synset} of {@code ancestorTerm}
     * @param ancestorTerm A term which might have a {@link Synset} that is a
     *        parent of a {@link Synset} of {@code childTerm}
     */
    public static HypernymStatus getHypernymStatus(Synset[] childSynsets,
                                                   Synset[] ancestorSynsets) {
        // Return false if either term is not found in wordnet.
        if ((childSynsets == null || childSynsets.length == 0) &&
            (ancestorSynsets == null || ancestorSynsets.length == 0))
            return HypernymStatus.TERMS_MISSING;
        
        // If the child does not have any synsets, but the other term does, we
        // consider this a novel hyponym pair.
        if (childSynsets == null || childSynsets.length == 0)
            return HypernymStatus.NOVEL_HYPERNYM;

        // If the ancestor does not have any synsets, but the other term does,
        // we consider this a novel hypernym pair.
        if (ancestorSynsets == null || ancestorSynsets.length == 0)
            return HypernymStatus.NOVEL_HYPONYM;

        // Compute the set of known ancestors for all synsets of the child term.
        Set<Synset> knownParents = new HashSet<Synset>();
        for (Synset childSynset : childSynsets)
            for (List<Synset> parents : childSynset.getParentPaths())
                knownParents.addAll(parents);

        // Remove the child synsets.
        for (Synset childSynset : childSynsets)
            knownParents.remove(childSynset);

        // If any of the synsets for the possible ancestor term are in the known
        // ancestor set, report that these words are an instance of a known
        // hypernym relation.
        for (Synset ancestorSynset : ancestorSynsets)
            if (knownParents.contains(ancestorSynset))
                return HypernymStatus.KNOWN_HYPERNYM;

        // Otherwise, report that no valid relationship was found.
        return HypernymStatus.KNOWN_NON_HYPERNYM;
    }

    /**
     * Returns the deepest {@link Synset} that subsumes both {@code synset1} and
     * {@code synset2}.
     */
    public static Synset lowestCommonHypernym(Synset synset1, Synset synset2) {
        List<Synset> subsumers = lowestCommonHypernyms(synset1, synset2);
        return (subsumers.size() == 0) ? null : subsumers.get(0);
    }

    /**
     * Returns the set of deepest {@link Synset}s that subsumes both {@code
     * synset1} and {@code synset2}.  Each returned {@link Synset} has the same
     * depth.
     */
    public static List<Synset> lowestCommonHypernyms(Synset synset1,
                                                     Synset synset2) {
        // Get the set of full parent paths for both synsets.
        List<List<Synset>> synset1Parents = synset1.getParentPaths();
        List<List<Synset>> synset2Parents = synset2.getParentPaths();

        // Compute the best distance from each ancestor synset of synset 1 to
        // the root synset.
        Map<Synset, Integer> bestDepthMap = new HashMap<Synset, Integer>();
        for (List<Synset> parents : synset1Parents) {
            int currDepth = 0;
            for (Synset parent : parents) {
                // Save the longest distance.
                Integer depth = bestDepthMap.get(parent);
                bestDepthMap.put(parent, (depth == null)
                        ? currDepth
                        : Math.max(depth, currDepth));
                currDepth++;
            }
        }


        // Compute the set of synsets that occur as ancestors for both synset1
        // and synset2 and update their best distances from the root of the
        // hierarchy.
        Set<Synset> candidates = new HashSet<Synset>();
        for (List<Synset> parents : synset2Parents) {
            int currDepth = -1;
            for (Synset parent : parents) {
                currDepth++;
                Integer depth = bestDepthMap.get(parent);

                // Ignore synsets that are not common ancestors.
                if (depth == null)
                    continue;

                // Update the distance for the common ancestor.
                candidates.add(parent);
                bestDepthMap.put(parent, Math.max(depth, currDepth));
            }
        }


        // Find the common ancestor which has the largest depth based on
        // distance from the root of the hierarchy. 
        int bestDepth = 0;
        for (Synset parent : candidates) 
            bestDepth = Math.max(bestDepth, bestDepthMap.get(parent));

        List<Synset> subsumers = new ArrayList<Synset>();
        for (Synset parent : candidates) {
            Integer depth = bestDepthMap.get(parent);
            if (depth == bestDepth)
                subsumers.add(parent);
        }
        return subsumers;
    }

    /**
     * Returns the length of the shortest path connecting {@code synset1} and
     * {@code synset2}.
     */
    public static int shortestPathDistance(Synset synset1, Synset synset2) {
        return pathDistance(synset1, synset2, false);
    }

    /**
     * Returns the length of the longest path connecting {@code synset1} and
     * {@code synset2}.
     */
    public static int longestPathDistance(Synset synset1, Synset synset2) {
        return pathDistance(synset1, synset2, true);
    }

    /**
     * Returns a {@link Pair} of {@link Integer}s that specifies the the minimum
     * distances from a {@link Synset} and {@code term1} a {@link Synset} of
     * {@code term2} to lowest common parent {@link Synset} which creates the
     * shorest path between the two synsets.  This method will compute all
     * possible {@link Synset} combinations of {@code term1Synsets} and {@code
     * term2Synsets} to find this optimal distance.  If either optimal distance
     * is of length greater than {@code maxDepth} it will be repaced with {@link
     * Integer#MAX_VALUE}.
     *
     * </p>
     *
     * This distance can be interpreted as the distance at which {@code term1}
     * and {@code term2} can be considered cousins.  For example, a cousin depth
     * of "5-4" signifies that some {@link Synset} of {@code term1} is 5 nodes
     * away from a common hypernym and some {@link Synset} of {@code term2} is 4
     * nodes away from the same common hypernym.  This common hypernym is
     * selected such that the total path, 5+4, is the shortest path connecting
     * these two term {@link Synset}s.
     *
     * @param term1Synsets The {@link Synset}s of the first term consider in a
     *        cousin relationship
     * @param term2Synsets The {@link Synset}s of the second term consider in a
     *        cousin relationship
     * @param maxDepth The maximal depth for cousin relationships
     */
    public static Pair<Integer> getCousinDistance(Synset[] term1Synsets,
                                                  Synset[] term2Synsets,
                                                  int maxDepth) {
        // Use a distance cache so that parent distances from the term2 synsets
        // do not need to be computed multiple times.
        Map<Synset, Map<Synset, Integer>> distanceCache =
            new HashMap<Synset, Map<Synset, Integer>>();

        // Precompute the worst case result.
        int bestDepth = Integer.MAX_VALUE;
        Pair<Integer> bestPair = new Pair<Integer>(
                Integer.MAX_VALUE, Integer.MAX_VALUE);

        // Compute the pairwise combinations of the synsets for both terms.
        // For each pairing, find the hypernym that creates the shortest path
        // between the two synsets.
        for (Synset term1Synset : term1Synsets) {
            Map<Synset, Integer> distanceMap1 = parentDistances(
                    term1Synset, false);
            bestPair = getCousinDistance(term1Synset, term2Synsets, 
                                         distanceMap1, distanceCache,
                                         bestDepth);
            bestDepth = bestPair.x + bestPair.y;
        }

        // If the best pair found went beyonds the maximal depth, set the values
        // to infinity and beyond.
        if (bestPair.x > maxDepth)
            bestPair = new Pair<Integer>(Integer.MAX_VALUE, bestPair.y);
        if (bestPair.y > maxDepth)
            bestPair = new Pair<Integer>(bestPair.y, Integer.MAX_VALUE);

        return bestPair;
    }

    private static Pair<Integer> getCousinDistance(
            Synset term1Synset,
            Synset[] term2Synsets,
            Map<Synset, Integer> distanceMap1,
            Map<Synset, Map<Synset, Integer>> distanceCache,
            int bestDepth) {
        Pair<Integer> bestPair = new Pair<Integer>(
                Integer.MAX_VALUE, Integer.MAX_VALUE);

        // Iterate through the synsets for term 2.
        for (Synset term2Synset : term2Synsets) {
            // Get the parent distances for term 2's synsets from the cache if
            // available, otherwise compute it and store in the cache.
            Map<Synset, Integer> distanceMap2 = distanceCache.get(term2Synset);
            if (distanceMap2 == null) {
                distanceMap2 = parentDistances(term2Synset, false);
                distanceCache.put(term2Synset, distanceMap2);
            }

            // Find the synsets which occur in both paths.
            Set<Synset> intersection = new HashSet<Synset>();
            Set<Synset> parentSet1 = distanceMap1.keySet();
            Set<Synset> parentSet2 = distanceMap2.keySet();
            for (Synset parent1 : parentSet1)
                if (parentSet2.contains(parent1))
                    intersection.add(parent1);

            // For the intersecting ancestors, find the ancestor which produces
            // the shortest path between the two selected term synsets.
            for (Synset shared : intersection) {
                Integer depth1 = distanceMap1.get(shared);
                Integer depth2 = distanceMap2.get(shared);

                if (depth1 + depth2 < bestDepth) {
                    bestDepth = depth1 + depth2;
                    bestPair = new Pair<Integer>(depth1, depth2);
                }
            }
        }
        return bestPair;
    }

    /**
     * Returns either the longest or shortest the path distance between two
     * given synsets when using only the hypernym/hyponym links.  If {@code
     * computeMax} is true, this returns the longest path distance, otherwise it
     * returns the shortest path distance.
     *
     * @param synset1 The origin of synset paths
     * @param synset2 The destination of synset paths
     * @param computeMax If true, the maximum distance between {@code synset1}
     *        and {@code synset2} will be returned, otherwise the minimum
     *        distance will be returned
     */
    private static int pathDistance(Synset synset1,
                                    Synset synset2,
                                    boolean computeMax) {
        if (synset1 == null || synset2 == null)
            return -1;

        // Compute the distance from each synset to all of it's ancestors.  The
        // resulting maps will include only the largest (or smallest) distance
        // from the synset to the ancestor.
        Map<Synset, Integer> distanceMap1 = parentDistances(
                synset1, computeMax);
        Map<Synset, Integer> distanceMap2 = parentDistances(
                synset2, computeMax);

        // Compute the set of synsets that occur as ancestors for both synsets.
        Set<Synset> intersection = new HashSet<Synset>();
        Set<Synset> parentSet1 = distanceMap1.keySet();
        Set<Synset> parentSet2 = distanceMap2.keySet();
        for (Synset parent1 : parentSet1)
            if (parentSet2.contains(parent1))
                intersection.add(parent1);

        // Find the synset which creates the largest (or shortest) path between
        // the two synsets based on the ancestor intersection set.
        int bestDepth = (computeMax) ? 0 : Integer.MAX_VALUE;
        for (Synset shared : intersection) {
            Integer depth1 = distanceMap1.get(shared);
            Integer depth2 = distanceMap2.get(shared);

            bestDepth = (computeMax)
                ? Math.max(bestDepth, depth1 + depth2)
                : Math.min(bestDepth, depth1 + depth2);
        }

        return (bestDepth == Integer.MAX_VALUE) ? -1 : bestDepth;
    }

    /**
     * Returns a mapping from a {@link Synset} to it's maximum (or minimum)
     * distance to the given {@link Synset}.
     *
     * @param synset The synset from which paths should begin
     * @param findMax If true, the mapping will contain the maximum distances,
     *        otherwise it will contain the minimum distances.
     */
    public static Map<Synset, Integer> parentDistances(Synset synset,
                                                       boolean findMax) {
        Map<Synset, Integer> distanceMap = new HashMap<Synset, Integer>();
        Set<Synset> seenNodes = new HashSet<Synset>();
        seenNodes.add(synset);
        distanceMap.put(synset, 0);
        for (Synset parent : synset.getParents())
            parentDistances(parent, distanceMap, seenNodes, 1, findMax);
        return distanceMap;
    }

    /**
     * Recursively populates the distance mapping with the maximum (or minimum)
     * distance to the given synset.
     * 
     * @param synset The synset from which distance paths should originate
     * @param distances A mapping from {@link Synset}s to their distances from a
     *        root node.
     * @param depth The current depth of {@code synset}
     * @param findMax If true, {@code distances} will be populated with the
     *        maximal distances, otherwise it will be populated with the minimal
     *        distances
     */
    private static void parentDistances(Synset synset,
                                        Map<Synset, Integer> distances,
                                        Set<Synset> seenNodes,
                                        int depth,
                                        boolean findMax) {
        if (seenNodes.contains(synset))
            return;
        seenNodes.add(synset);

        Integer oldDepth = distances.get(synset);
        distances.put(synset, (oldDepth == null)
                ? depth
                : (findMax)
                  ? Math.max(oldDepth, depth)
                  : Math.min(oldDepth, depth));
        for (Synset parent : synset.getParents())
            parentDistances(parent, distances, seenNodes, depth+1, findMax);
        seenNodes.remove(synset);
    }

    /**
     * Returns the {@link Synset}, the liklihood of this being the best parent,
     * which is the best attachment point based on the set of possible hypernym
     * lemmas.  If some new term needs to be added to WordNet, and one has
     * probabilities that the new term is a hyponym of some terms or the cousin
     * of other terms, this method selects the {@link Synset} corresponding to
     * the known term that is most likely the hypernym.    
     *
     * @param attachmentLocations The array of words related to the to be added
     *        term through either a potential hypernym relationship or cousin
     *        relationship
     * @param attachmentScores The probability that each potentially related
     *        terms is a hypernym of the to be added term
     * @param cousinScores A mapping for each potentially related terms of
     *        cousin depths to the probabilty of the related term being having
     *        that cousin relationship with the to be added term.    Keys the
     *        maps must be of the form INT-INT, with the largest value being
     *        first.
     * @param lambda A decay parameter.  If a term in {@code
     *        attachmentLocations} is a potential hypernym, and other terms in
     *        {@code attachmentLocations} are hypernyms of this potential term,
     *        then this lambda value is used to modify the likelihood of the
     *        implied reliationships
     */
    public static Duple<Synset, Double> bestAttachmentPoint(
            OntologyReader wordnet,
            String[] attachmentLocations,
            double[] attachmentScores,
            Map<String, Double> cousinScores,
            double lambda) {
        // Create all of the synsets at which a given term may be attached.
        // Computing a mapping from those synsets to the evidence score for that
        // particular attachment.  Note that all synsets generated from the same
        // lemma will have the same attachment score.
        Map<Synset, Double> synsetToHypernymScore =
            new HashMap<Synset, Double>();
        Synset[][] attachmentSynsets =
            new Synset[attachmentLocations.length][0];
        int i = 0;
        for (String attachment : attachmentLocations) {
            attachmentSynsets[i] = wordnet.getSynsets(
                    attachment, PartsOfSpeech.NOUN);
            for (Synset synset : attachmentSynsets[i])
                synsetToHypernymScore.put(synset, attachmentScores[i]);
            i++;
        }

        // Create a cache of distance values for the cousin distances.
        Map<Synset, Map<Synset, Integer>> distanceCache =
            new HashMap<Synset, Map<Synset, Integer>>();

        // Create the default best values.
        double bestDelta = 0;
        Synset bestLocation = null;

        for (i = 0; i < attachmentSynsets.length; ++i) {
            Synset[] possibleParents = attachmentSynsets[i];
            // Compute the score for attaching the word to the current possible
            // attachment.
            double addScore = computeScore(1, attachmentScores[i], lambda);

            for (Synset possibleParent : possibleParents) {
                // Compute the possible implied relations.
                List<List<Synset>> parentPaths = possibleParent.getParentPaths();

                // Iterate through the set of implied parents and determine if
                // there is any hypernym evidence for for them.  If so, compute
                // the score for adding that relation to the taxonomy.
                double impliedScore = 1;
                for (List<Synset> parents : parentPaths) {
                    int depth = parents.size();
                    for (Synset parent : parents) {
                        depth--;
                        Double evidenceScore = synsetToHypernymScore.get(
                                parent);
                        if (evidenceScore != null)
                            impliedScore *= computeScore(
                                    depth, evidenceScore, lambda);
                    }
                }

                double totalCousinScore = 1;
                /*
                // Compute the probability of any implied cousin relationships.
                int c = 0;
                // Iterate through each of the words with cousin score
                // information. Try to find a link between the synsets for this
                // cousin term and the possible attachment point we are making.
                // If a valid cousin relationship exists, include the implied
                // probability of the cousin relationship.
                for (Map.Entry<String, Double> score :
                        cousinScores.entrySet()) {
                    Synset[] cousins = wordnet.getSynsets(
                            score.getKey(), PartsOfSpeech.NOUN);

                    // Skip empty cousins.
                    if (cousins == null || cousins.length == 0)
                            continue;

                    // Get the best distances found for the synset and the
                    // cousins.
                    Pair<Integer> bestCousinDistance = getCousinDistance(
                            possibleParent, cousins,
                            parentDistances(possibleParent, false), 
                            distanceCache, Integer.MAX_VALUE);

                    // Skip cousins that have no valid path.
                    if (bestCousinDistance == null)
                        continue;
                    if (bestCousinDistance.x == Integer.MAX_VALUE ||
                            bestCousinDistance.y == Integer.MAX_VALUE)
                            continue;

                    totalCousinScore *= computeScore(1, score.getValue(), 1);
                }
                */

                // If the full score observed is the best so far, save the delta
                // and the parent which generated this delta.
                double totalScore = addScore * impliedScore * totalCousinScore;
                if (totalScore >= bestDelta) {
                    bestDelta = totalScore;
                    bestLocation = possibleParent;
                }
            }
        }

        return new Duple<Synset, Double>(bestLocation, bestDelta);
    }

    /**
     * Returns a score for the given probability based on the depth of the term
     * that generated the score and a decay factor.
     */ 
    private static double computeScore(int depth, double score, double lambda) {
        double modifiedProb = Math.pow(lambda, depth-1) * score;
        return modifiedProb / (1.000001 - modifiedProb);
    }

    public static Duple<Synset, Double> bestAttachmentPointWithError(
            OntologyReader wordnet,
            String[] attachmentLocations,
            double[] attachmentScores,
            double lambda) {
        // Create all of the synsets at which a given term may be attached.
        // Computing a mapping from those synsets to the evidence score for that
        // particular attachment.  Note that all synsets generated from the same
        // lemma will have the same attachment score.
        Map<Synset, Double> synsetToHypernymScore =
            new HashMap<Synset, Double>();
        Synset[][] attachmentSynsets =
            new Synset[attachmentLocations.length][0];
        int i = 0;
        for (String attachment : attachmentLocations) {
            attachmentSynsets[i] = wordnet.getSynsets(
                    attachment, PartsOfSpeech.NOUN);
            for (Synset synset : attachmentSynsets[i])
                synsetToHypernymScore.put(synset, attachmentScores[i]);
            i++;
        }

        // Create a cache of distance values for the cousin distances.
        Map<Synset, Map<Synset, Integer>> distanceCache =
            new HashMap<Synset, Map<Synset, Integer>>();

        // Create the default best values.
        double bestDelta = Integer.MAX_VALUE;
        Synset bestLocation = null;

        for (i = 0; i < attachmentSynsets.length; ++i) {
            for (Synset possibleParent : attachmentSynsets[i]) {
                // Iterate through the set of implied parents and determine if
                // there is any hypernym evidence for for them.  If so, compute
                // the score for adding that relation to the taxonomy.
                double impliedScore = 1;
                for (List<Synset> parents : possibleParent.getParentPaths()) {
                    int depth = parents.size();
                    for (Synset parent : parents) {
                        Double evidenceScore = synsetToHypernymScore.get(
                                parent);
                        if (evidenceScore != null)
                            impliedScore *= computeError(
                                    depth, evidenceScore, lambda);
                        depth--;
                    }
                }

                // If the full score observed is the best so far, save the delta
                // and the parent which generated this delta.
                if (impliedScore <= bestDelta) {
                    bestDelta = impliedScore;
                    bestLocation = possibleParent;
                }
            }
        }

        return new Duple<Synset, Double>(bestLocation, bestDelta);
    }

    private static double computeError(int depth, double score, double lambda) {
        double modifiedProb = Math.pow(lambda, depth-1) * score;
        return (1.000001 - modifiedProb) / modifiedProb;
    }

}
