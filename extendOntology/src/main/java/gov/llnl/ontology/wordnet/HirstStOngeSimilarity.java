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

import gov.llnl.ontology.wordnet.Synset.Relation;


/**
 * Implements a word net path similarity measure defined by Hirst and St-Onge.
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
 *
 * @author Keith Stevens
 */
public class HirstStOngeSimilarity implements SynsetSimilarity {

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
     * {@inheritDoc}
     */
    public double similarity(Synset synset1, Synset synset2) {
        double bestScore = 0;
        for (Relation relation : UP_RELATION)
            for (Synset related : synset1.getRelations(relation))
                bestScore = Math.max(
                        bestScore, hsoState1(1, 0, related, synset2));

        for (Relation relation : DOWN_RELATION)
            for (Synset related : synset1.getRelations(relation))
                bestScore = Math.max(
                        bestScore, hsoState3(1, 0, related, synset2));

        for (Relation relation : SIDE_RELATION)
            for (Synset related : synset1.getRelations(relation))
                bestScore = Math.max(
                        bestScore, hsoState2(1, 0, related, synset2));

        // With k == 1 the minimum score possible is 0 and the maximum
        // score is C.  Divide by C so that it's within a range of 0 to 1.
        return bestScore / C;
    }

    /**
    * Traverses valid outward links from {@code src} node.  If {@code src}
    * equals {@code dest}, then a score is computed.  Of all possible paths from
    * this state, the maximum score is returned.
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
    * Traverses valid outward links from {@code src} node.  If {@code src}
    * equals {@code dest}, then a score is computed.  Of all possible paths from
    * this state, the maximum score is returned.
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
    * Traverses valid outward links from {@code src} node.  If {@code src}
    * equals {@code dest}, then a score is computed.  Of all possible paths from
    * this state, the maximum score is returned.
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
    * Traverses valid outward links from {@code src} node.  If {@code src}
    * equals {@code dest}, then a score is computed.  Of all possible paths from
    * this state, the maximum score is returned.
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
    * Traverses valid outward links from {@code src} node.  If {@code src}
    * equals {@code dest}, then a score is computed.  Of all possible paths from
    * this state, the maximum score is returned.
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
