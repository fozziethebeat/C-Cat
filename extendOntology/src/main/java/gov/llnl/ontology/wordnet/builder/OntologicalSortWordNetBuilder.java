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

import gov.llnl.ontology.wordnet.BaseLemma;
import gov.llnl.ontology.wordnet.BaseSynset;
import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.SynsetRelations;
import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;
import gov.llnl.ontology.wordnet.Synset.Relation;
import gov.llnl.ontology.wordnet.OntologyReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * @author Keith Stevens
 */
public class OntologicalSortWordNetBuilder implements WordNetBuilder {

    private final OntologyReader wordnet;

    private Set<String> knownTerms;

    private List<TermToAdd> termsToAdd;

    public OntologicalSortWordNetBuilder(OntologyReader wordnet) {
        this.wordnet = wordnet;
        knownTerms = new HashSet<String>();
        termsToAdd = new ArrayList<TermToAdd>();
    }

    public void addEvidence(String child,
                            String[] parents,
                            double[] parentScores,
                            Map<String, Double> cousinScores) {
        knownTerms.add(child);
        termsToAdd.add(new TermToAdd(
                    child, parents, parentScores, cousinScores));
    }

    public void addTerms(OntologyReader wordnet) {
        for (TermToAdd termToAdd : termsToAdd) {
            termToAdd.checkParentsInWordNet(wordnet);
            termToAdd.checkParentsInList(knownTerms);
        }
        Collections.sort(termsToAdd);
        for (TermToAdd termToAdd : termsToAdd) {
            Duple<Synset,Double> bestAttachment = 
                SynsetRelations.bestAttachmentPoint(
                        termToAdd.parents, termToAdd.parentScores,
                        termToAdd.cousinScores, .05);
            Synset newSynset = new BaseSynset(PartsOfSpeech.NOUN);
            newSynset.addLemma(new BaseLemma(newSynset, termToAdd.term,
                                             "", 0, 0, "n"));
            newSynset.addRelation(Relation.HYPERNYM, bestAttachment.x);
            wordnet.addSynset(newSynset);

            System.out.printf("Adding %s -> %s\n",
                              termToAdd.term, bestAttachment.x.getSenseKey());
        }
    }
}
