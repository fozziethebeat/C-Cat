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

import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.WordNetCorpusReader;
import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;

import gov.llnl.ontology.wordnet.feature.ExtendedSnowEtAlFeatureMaker;
import gov.llnl.ontology.wordnet.feature.OntologicalFeatureMaker;
import gov.llnl.ontology.wordnet.feature.StandardFeatureMaker;
import gov.llnl.ontology.wordnet.feature.SynsetPairFeatureMaker;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceIO;

import edu.ucla.sspace.clustering.Assignment;
import edu.ucla.sspace.clustering.Clustering;
import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering;

import edu.ucla.sspace.matrix.Matrices;

import edu.ucla.sspace.util.Pair;
import edu.ucla.sspace.util.ReflectionUtil;

import edu.ucla.sspace.vector.DoubleVector;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * This runnable class creates a series of feature vectors for every {@link
 * synset} reachable from the noun "entity" {@link Synset}.  Competing {@link
 * Synset}s for every noun lemma are merged using agglomerative merging.  The
 * similarity between any two {@link Synset}s is the average simiarlity of the
 * created feature vectors for the two {@link Synset}s, along with any
 * additional features created by a {@link SynsetPairFeatureMaker}.  The
 * resulting WordNet hierarchy is then saved to disk.
 *
 * @author Keith Stevens
 */
public class WordNetCondenser {

    /**
     * The {@link SynsetPairFeatureMaker} implementations that are usable by
     * this {@link WordNetCondenser}.
     */
    public enum DataSet {
        SNOW_EXTENDED,
        STANDARD,
    }

    /**
     * Performs the merge.
     */
    public static void main(String[] args) throws IOException {
        // Create the command line arguments.
        ArgOptions options = new ArgOptions();
        options.addOption('s', "sspaceFiles",
                          "Specifies a series of sspace file and, " +
                          "optionally, term weights, that should be used " +
                          "to generate extra features.",
                          true, "(<sspace>[:<featureWeight>])[,\\1]*",
                          "Required");
        options.addOption('r', "rootLemma",
                          "The lemma specifying the highest synset to which " +
                          "clustering be limited. (Default: entity)",
                          true, "STRING", "Optional");
        options.addOption('f', "featureSetType",
                          "Specifies the type of data set to generate.",
                          true, "SNOW_EXTENDED|STANDARD", "Required");
        options.parseOptions(args);

        // Check that the required arguments are given.
        if (options.numPositionalArgs() != 2) {
            System.out.println(
                    "usage: java WordNetCondenser [options] " +
                    "<dictPath> <newWordNetDir>\n" +
                    options.prettyPrint());
            System.exit(1);
        }

        // Get the directory information needed.
        String wordNetDir = options.getPositionalArg(0);
        String newWordNetDir = options.getPositionalArg(1);

        // Create the word net reader and the root synset.
        WordNetCorpusReader wordnet = WordNetCorpusReader.initialize(
                wordNetDir);
        Synset root = (options.hasOption('r'))
            ? wordnet.getSynsets(options.getStringOption('r'))[1]
            : wordnet.getSynset("entity", PartsOfSpeech.NOUN, 1);

        // For each ssace file specified by the command line, load the sspace,
        // load feature weights of provided, and create ontological vectors for
        // every reachable synset.  Store the attribute labels so that the
        // SynsetPairFeatureMaker can access the feature vectors.
        List<String> synsetLabels = new ArrayList<String>();
        for (String item : options.getStringOption('s').split(",")) {
            // Load the semantic space and the weights.
            String[] sspaceAndWeight = item.split(":");
            SemanticSpace sspace = SemanticSpaceIO.load(sspaceAndWeight[0]);
            Map<String, Double> weights = (sspaceAndWeight.length == 2)
                ? makeWeightMap(sspaceAndWeight[1]) : null;

            // Create the feature vectors and store the attribute label.
            OntologicalFeatureMaker maker = new OntologicalFeatureMaker(
                    sspace, weights);
            maker.induceOntologicalFeatures(
                    wordnet.getSynset("entity", PartsOfSpeech.NOUN, 1));
            synsetLabels.add(maker.getAttributeName());
        }
 
        // Create the feature maker for synset pairs.
        DataSet setType = DataSet.valueOf(
                options.getStringOption('f').toUpperCase());
        SynsetPairFeatureMaker featureMaker;
        switch (setType) {
            case SNOW_EXTENDED:
                featureMaker = new ExtendedSnowEtAlFeatureMaker(
                        wordnet, synsetLabels);
                break;
            case STANDARD:
                featureMaker = new StandardFeatureMaker(synsetLabels);
                break;
            default:
                featureMaker = null;
        }

        // Condense the word net hierarchy and store it to disk.
        condense(root, featureMaker);
        wordnet.saveWordNet(newWordNetDir);
    }

    /**
     * Returns a mapping from terms to their weights.
     */
    private static Map<String, Double> makeWeightMap(String featureWeightFile) 
            throws IOException {
        Map<String, Double> weights = new HashMap<String, Double>();
        BufferedReader br = new BufferedReader(new FileReader(
                    featureWeightFile));
        for (String line = null; (line = br.readLine()) != null; ) {
            String[] tokens = line.split(" ");
            weights.put(tokens[0], Double.parseDouble(tokens[1]));
        }
        return weights;
    }

    /**
     * Agglomeratively clusters the competing {@link Synset}s for each lemma
     * in WordNet.    The similarity between any two {@link Synset}s the average
     * value of the feature vector generated by the {@code featureMaker}.
     */
    public static void condense(Synset root,
                                SynsetPairFeatureMaker featureMaker) {
        System.out.println("Clustering ambigious words");

        WordNetCorpusReader wordnet = WordNetCorpusReader.getWordNet();

        // Iterate over every lemma.
        Set<String> wordnetLemmas = wordnet.wordnetTerms();
        for (String lemma : wordnetLemmas) {

            // Only combine the competing senses that share the same part of
            // speech.
            for (PartsOfSpeech pos : PartsOfSpeech.values()) {
                // Skip any lemmas that have no senses for this part of speech.
                Synset[] synsets = wordnet.getSynsets(lemma, pos);
                if (synsets.length == 0)
                    continue;
                
                // Merge the two most similar synsets until the most similar
                // synsets are less similar than some threshold.
                double bestSimilarity = 1;
                while (bestSimilarity > .50) {
                    bestSimilarity = 0;
                    Pair<Synset> bestPair = null;

                    // Inspect every pair wise combination of synsets.
                    for (int i = 0; i < synsets.length; ++i) {
                        for (int j = 0; j < synsets.length; ++j) {
                            // Get the feature similarity of the two synsets. 
                            DoubleVector pairFeatureVector = 
                                featureMaker.makeFeatureVector(
                                        synsets[i], synsets[j]);

                            // Create the average similarty based on the feature
                            // vector.
                            double score = 0;
                            for (int index = 0;
                                 index < pairFeatureVector.length(); ++index)
                                score += pairFeatureVector.get(index);
                            score /= (double) pairFeatureVector.length();

                            // Store the two most similar synsets.
                            if (score > bestSimilarity) {
                                bestSimilarity = score;
                                bestPair = 
                                    new Pair<Synset>(synsets[i], synsets[j]);
                            }
                        }
                    }

                    // Merge the most similar synsets.
                    bestPair.x.merge(bestPair.y);

                    // Update the synset list after the merge.
                    synsets = wordnet.getSynsets(lemma, pos);
                }
            }
        }
    }
}
