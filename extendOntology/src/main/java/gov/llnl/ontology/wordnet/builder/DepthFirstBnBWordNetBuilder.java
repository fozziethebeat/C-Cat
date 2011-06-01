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

package gov.llnl.ontology.wordnet.builder;

import edu.ucla.sspace.util.Duple;

import gov.llnl.ontology.mains.BuilderScorer;

import gov.llnl.ontology.wordnet.BaseLemma;
import gov.llnl.ontology.wordnet.BaseSynset;
import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.SynsetRelations;
import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;
import gov.llnl.ontology.wordnet.Synset.Relation;
import gov.llnl.ontology.wordnet.OntologyReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Deque;


/**
 * @author Keith Stevens
 */
public class DepthFirstBnBWordNetBuilder implements WordNetBuilder {

    private final OntologyReader wordnet;

    private Set<String> knownTerms;

    private List<TermToAdd> termsToAdd;

    private boolean compareInWn;

    private boolean order;

    public DepthFirstBnBWordNetBuilder(OntologyReader wordnet,
                                       boolean compareInWn,
                                       boolean order) {
        this.wordnet = wordnet;
        this.compareInWn = compareInWn;
        this.order = order;
        knownTerms = new HashSet<String>();
        termsToAdd = new ArrayList<TermToAdd>();
    }

    public void addEvidence(String child,
                            String[] parents,
                            double[] parentScores,
                            Map<String, Double> cousinScores) {
        knownTerms.add(child);
        termsToAdd.add(new TermToAdd(
                    child, parents, parentScores, cousinScores, compareInWn));
    }

    public void addTerms(OntologyReader wordnet, BuilderScorer scorer) {
        // Do an ontologoical sort on the set of terms that need to be added
        // such that words with more parents already in wordnet are ordered
        // first and words with fewer parents are last.
        for (TermToAdd termToAdd : termsToAdd) {
            termToAdd.checkParentsInWordNet(wordnet);
            termToAdd.checkParentsInList(knownTerms);
        }

        if (order)
            Collections.sort(termsToAdd);

        // If any words lack any possible parents remove them from the list
        // entirely as they could never be added.
        int zeroParentIndex = 0;
        for (TermToAdd termToAdd : termsToAdd) {
            zeroParentIndex++;
            if (termToAdd.numParentsInWordNet + termToAdd.numParentsToAdd == 0)
                break;
        }
        if (zeroParentIndex < termsToAdd.size())
            termsToAdd = termsToAdd.subList(0, zeroParentIndex);

        // Create the data structures needed during the branch and bound search.
        Set<TermToAdd> seen = new HashSet<TermToAdd>(termsToAdd.size());
        Deque<Synset> addList = new LinkedList<Synset>();
        List<Synset> finalList = new ArrayList<Synset>(termsToAdd.size());

        // Do the search with nothing added.
        addTerm(wordnet, seen, addList, finalList, Integer.MAX_VALUE, 0, scorer);
    }

    private double addTerm(OntologyReader wordnet, 
                           Set<TermToAdd> seen, 
                           Deque<Synset> addList,
                           List<Synset> finalList,
                           double maxCost,
                           double currCost,
                           BuilderScorer scorer) {
        // If we've added everything we need too or can add, then we've reached
        // a valid goal node.  Return the cost we've been given and copy over
        // the order of the synsets added.
        if (termsToAdd.size() == seen.size()) {
            finalList.clear();
            finalList.addAll(addList);
            scorer.scoreAdditions(wordnet);
            return currCost;
        }

        for (TermToAdd termToAdd : termsToAdd) {
            // If we've already seen this word in previous recursive calls, skip
            // it.
            if (seen.contains(termToAdd))
                continue;

            // Find the best attachment point for the given word based.
            Duple<Synset,Double> bestAttachment = 
                SynsetRelations.bestAttachmentPointWithError(
                        termToAdd.parents, termToAdd.parentScores, .95);

            // Compute the cost of this attachment, i.e how likely that it is
            // wrong and how likely that the other attachments are wrong.
            double likelihood = bestAttachment.y;
            double newCost = currCost + likelihood;

            // If the new cost is higher than the best cost found so far, reject
            // this addition and move along.  We don't need to explore further
            // down this ordering of additions.
            if (newCost > maxCost)
                continue;

            // Add this synset to the tree and then recursively try to add more
            // synsets.
            Synset newSynset = new BaseSynset(PartsOfSpeech.NOUN);
            newSynset.addLemma(new BaseLemma(newSynset, termToAdd.term,
                                             "", 0, 0, "n"));
            newSynset.addRelation(Relation.HYPERNYM, bestAttachment.x);

            // Add this synset.
            wordnet.addSynset(newSynset);
            seen.add(termToAdd);
            addList.push(newSynset);

            // Recursively try to add the rest of the synsets.
            newCost = addTerm(wordnet, seen, addList, 
                              finalList, maxCost, newCost, scorer);

            // Remove the synset from the tree so that the next call does not
            // observe this addition.
            addList.pop();
            seen.remove(termToAdd);
            wordnet.removeSynset(newSynset);

            // Update the best cost found so far.
            maxCost = Math.min(newCost, maxCost);
        }

        // Once we've reached this point, we've explored all possibilities of
        // adding the nodes that have not yet been added for this recursive
        // call.  Return the best cost found so far.
        return maxCost;
    }
}
