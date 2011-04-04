/*
 * Copyright (c) 2011, Lawrence Livermore National Security, LLC. Produced at
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

package gov.llnl.ontology.mains;

import gov.llnl.ontology.util.MahoutSparseVector;

import gov.llnl.ontology.wordnet.SynsetRelations;
import gov.llnl.ontology.wordnet.SynsetRelations.HypernymStatus;

import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.StaticSemanticSpace;

import edu.ucla.sspace.dependency.CoNLLDependencyExtractor;
import edu.ucla.sspace.dependency.ConjunctionTransform;
import edu.ucla.sspace.dependency.DependencyExtractor;
import edu.ucla.sspace.dependency.DependencyTreeNode;
import edu.ucla.sspace.dependency.DependencyTreeTransform;
import edu.ucla.sspace.dependency.DependencyPath;
import edu.ucla.sspace.dependency.FilteredDependencyIterator;
import edu.ucla.sspace.dependency.UniversalPathAcceptor;

import edu.ucla.sspace.dv.DependencyPathBasisMapping;
import edu.ucla.sspace.dv.RelationPathBasisMapping;

import edu.ucla.sspace.text.UkWacDependencyFileIterator;
import edu.ucla.sspace.text.Document;

import edu.ucla.sspace.util.Pair;

import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.Vector;

import org.apache.mahout.classifier.OnlineLearner;
import org.apache.mahout.classifier.sgd.AdaptiveLogisticRegression;
import org.apache.mahout.classifier.sgd.L1;
import org.apache.mahout.classifier.sgd.OnlineLogisticRegression;

import org.apache.mahout.ep.State;

import org.apache.mahout.math.DenseVector;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author Keith Stevens
 */
public class ExtendWordNet {

    private final DependencyExtractor extractor;

    private final DependencyPathBasisMapping basis;

    private final DependencyTreeTransform transformer;

    private final int numSimilarityScores;

    private final EvidenceMap knownPositives;

    private final EvidenceMap knownNegatives;

    private final EvidenceMap unknownEvidence;

    private OnlineLogisticRegression hypernymPredictor;

    private OnlineLogisticRegression cousinPredictor;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("usage: java ExtendWordNet <corpus> <sspace>+");
            System.exit(1);
        }

        ExtendWordNet builder = new ExtendWordNet(
                new CoNLLDependencyExtractor(),
                new RelationPathBasisMapping(),
                new ConjunctionTransform(),
                args.length - 1);
        Iterator<Document> corpusIter = new UkWacDependencyFileIterator(
                args[0]);
        while (corpusIter.hasNext())
            builder.gatherEvidence(corpusIter.next().reader());

        System.err.println("Evidence gathered");

        for (int s = 1; s < args.length; ++s) {
            SemanticSpace sspace = new StaticSemanticSpace(args[s]);
            builder.applySimilarityScores(s-1, sspace);
        }

        System.err.println("Similarity labeled");

        builder.trainModel(5);

        System.err.println("Models trained");

        builder.labelUnknownEvidence();

        System.err.println("Unknown evidence labeled");

        builder.extendWordNet();

        System.err.println("Words added to wordnet");

    }

    /**
     * Compute the hypernym and cousin probabilities for each unknown noun pair.
     */
    public void labelUnknownEvidence() {
        // Iterate through each of the first words in the relationships.
        for (Map.Entry<String, Map<String, Evidence>> fe :
                unknownEvidence.map().entrySet()) {
            // Iterate through each of the related words and the evidence which
            // describes the patterns between the first word and the second
            // word.
            for (Map.Entry<String, Evidence> entry : fe.getValue().entrySet()) {
                // The predictor return the probability that the data point lies
                // in the first class, which in both cases is the negative
                // class.  Find the positive probability by removing the given
                // score from 1.
                double hypernymProb = 1 - hypernymPredictor.classifyScalar(
                        new MahoutSparseVector(entry.getValue().vector));
                double cousinProb = 1 - cousinPredictor.classifyScalar(
                        new DenseVector(entry.getValue().similarityScores));
                entry.getValue().classScores = new ClassScores(
                        hypernymProb, cousinProb);
            }
        }
    }

    public void extendWordNet() {
    }

    /**
     * Constructs a new {@link ExtendWordNet} instance.
     */
    public ExtendWordNet(DependencyExtractor extractor,
                         DependencyPathBasisMapping basis,
                         DependencyTreeTransform transformer,
                         int numSimilarityScores) {
        this.basis = basis;
        this.extractor = extractor;
        this.transformer = transformer;
        this.numSimilarityScores = numSimilarityScores;

        knownPositives = new EvidenceMap(basis);
        knownNegatives = new EvidenceMap(basis);
        unknownEvidence = new EvidenceMap(basis);

    }

    /**
     * Applies similarity scores for each of the word pairs in the evidence
     * maps.
     */
    public void applySimilarityScores(int sspaceNum, SemanticSpace sspace) {
        knownPositives.scorePairs(sspaceNum, sspace);
        knownNegatives.scorePairs(sspaceNum, sspace);
        unknownEvidence.scorePairs(sspaceNum, sspace);
    }

    /**
     * Trains the hypernym and cousin predictor models based on the evidence
     * gathered for positive and negative relationships.  Returns true if both
     * models could be trained.
     */
    public boolean trainModel(int numPasses) {
        // Train the hypernym predictor.
        AdaptiveLogisticRegression model = new AdaptiveLogisticRegression(
                basis.numDimensions(), 2, new L1());
        for (int i = 0; i < numPasses; ++i) {
            trainHypernyms(knownPositives.map(), model, 1);
            trainHypernyms(knownNegatives.map(), model, 0);
        }

        // Get the best predictor for hypernyms from the trainer.  If no trainer
        // could be found, return false.
        State<AdaptiveLogisticRegression.Wrapper> best = model.getBest();
        if (best == null)
            return false;
        hypernymPredictor = best.getPayload().getLearner().getModels().get(0);

        // Train the cousin predictor using the similarity scores.
        model = new AdaptiveLogisticRegression(
                numSimilarityScores, 2, new L1());
        for (int i = 0; i < numPasses; ++i) {
            trainCousins(knownPositives.map(), model);
            trainCousins(knownNegatives.map(), model);
        }

        // Get the best cousin predictor model from the trainer.  If no trainer
        // could be found, return false.
        best = model.getBest();
        if (best == null)
            return false;
        cousinPredictor = best.getPayload().getLearner().getModels().get(0);

        return true;
    }

    /**
     * Iterates through the {@link DependencyPath} found in the dependency trees
     * found in {@code document}.  For any two nouns that are connected by some
     * {@link DependencyPath}, the shortest path connecting these two nouns will
     * be used as evidence.
     */
    public void gatherEvidence(BufferedReader document) {
        try {
            for (DependencyTreeNode[] tree = null;
                    (tree = extractor.readNextTree(document)) != null; ) {
                tree = transformer.transform(tree);

                for (DependencyTreeNode treeNode : tree) {
                    // Reject any nodes that are not nouns.
                    if (!treeNode.pos().startsWith("N"))
                        continue;

                    Set<DependencyTreeNode> seenNodes =
                        new HashSet<DependencyTreeNode>();

                    // Iterate through all of the available dependency paths
                    // starting at this current node.  For all valid paths, i.e.
                    // ones that start and end with nounds, and at least one of
                    // the nodes is in word net, emit the dependency path
                    // between the two nouns.
                    Iterator<DependencyPath> pathIter =
                        new FilteredDependencyIterator(
                                treeNode, new UniversalPathAcceptor(), 7);
                    while (pathIter.hasNext()) {
                        DependencyPath path = pathIter.next();
      
                        // Reject any end nodes that are not nouns.
                        if (!path.last().pos().startsWith("N"))
                            continue;

                        String firstTerm = path.first().word();
                        String secondTerm = path.last().word();

                        // Check to see if the current end node in the path has
                        // already been observed.  This allows us to select only
                        // the dependency path which is shortest between the two
                        // nodes.
                        if (seenNodes.contains(path.last()))
                            continue;

                        seenNodes.add(path.last());

                        addTermEvidence(firstTerm, secondTerm, path);
                    }
                }
            }
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Add the {@link DependencyPath} information connecting {@code firstTerm}
     * and {@code secondTerm} in the appropriate {@link EvidenceMap}.  If both
     * terms are in wordnet and {@code firstTerm} is a hypernym of {@code
     * secondTerm}, the data will be stored in {@code knownPositives}.  If they
     * are in wordnet and there is no hypernym relationship, the data will be
     * stored in {@code knownNegatives}.  Otherwise, the data will be stored in
     * {@code unknownEvidence}.
     */
    private void addTermEvidence(String firstTerm,
                                 String secondTerm,
                                 DependencyPath path) {
        // Store the data in the correct map if the two words have already been
        // seen.
        if (knownPositives.contains(firstTerm, secondTerm))
            knownPositives.add(firstTerm, secondTerm, path);
        else if (knownNegatives.contains(firstTerm, secondTerm))
            knownNegatives.add(firstTerm, secondTerm, path);
        else if (unknownEvidence.contains(firstTerm, secondTerm))
            unknownEvidence.add(firstTerm, secondTerm, path);
        else {
            // Otherwise determine the relationship between the two words and
            // then select the correct map.
            HypernymStatus hypernymEvidenceStatus = 
                SynsetRelations.getHypernymStatus(firstTerm, secondTerm);
            switch (hypernymEvidenceStatus) {
                case KNOWN_HYPERNYM:
                    knownPositives.add(firstTerm, secondTerm, path);
                    break;
                case KNOWN_NON_HYPERNYM:
                    knownNegatives.add(firstTerm, secondTerm, path);
                    break;
                default:
                    unknownEvidence.add(firstTerm, secondTerm, path);
            }
        }
    }

    /**
     * Trains the {@code hypernymPredictor} using the evidence from an {@link
     * EvidenceMap} and the class labels for each data point in that map.
     * 
     * @param map The raw {@link Map} holding {@link Evidence} instances
     * @param model The model to be trained
     * @param classLabel the class label to be applied to all data in {@code
     *        map}
     */
    private void trainHypernyms(Map<String, Map<String, Evidence>> map,
                                OnlineLearner model,
                                int classLabel) {
        for (Map<String, Evidence> value : map.values())
            for (Evidence e : value.values()) 
                model.train(classLabel, new MahoutSparseVector(e.vector));
    }

    /**
     * Trains the {@code cousinPredictor} using the evidence from an {@link
     * EvidenceMap} and the similarity scores for each data point in that map.
     * 
     * @param map The raw {@link Map} holding {@link Evidence} instances
     * @param model The model to be trained
     */
    private void trainCousins(Map<String, Map<String, Evidence>> map,
                              OnlineLearner model) {
        for (Map.Entry<String, Map<String, Evidence>> entry: map.entrySet()) {
            for (Map.Entry<String, Evidence> ev : entry.getValue().entrySet()) {
                // Determine whether or not the two terms are cousins.
                // Initially assume that they arent.
                Pair<Integer> cousinDepth = SynsetRelations.getCousinDistance(
                        entry.getKey(), ev.getKey(), 7);

                // If the two terms are cousins, use 1 as the class label,
                // otherwise use 0.
                int classLabel = 0;
                if (cousinDepth.x != Integer.MAX_VALUE &&
                    cousinDepth.y != Integer.MAX_VALUE)
                    classLabel = 1;

                // Train the model with this data point.
                model.train(classLabel,
                            new DenseVector(ev.getValue().similarityScores));
            }
        }
    }

    /**
     * A mapping from two terms to the observed {@link Evidence} information.
     */
    private class EvidenceMap {

        /**
         * The mapping from two connected words to their observed dependency
         * path information and semantic similarity scores.
         */
        private final HashMap<String, Map<String, Evidence>> map;

        /**
         * The {@link DependencyPathBasisMapping} used to associate {@link
         * DependencyPath}s with dimensions.
         */
        private final DependencyPathBasisMapping basis;

        /**
         * Constructs a new {@link EvidenceMap}.
         */
        public EvidenceMap(DependencyPathBasisMapping basis) {
            this.basis = basis;
            map = new HashMap<String, Map<String, Evidence>>();
        }

        /**
         * Adds the {@link DependencyPath} information connecting {@code term1}
         * and {@code term2} .
         */
        public void add(String term1, String term2, DependencyPath path) {
            // Get the mapping from term1 to other second terms.
            Map<String, Evidence> term1Map = map.get(term1);
            if (term1Map == null) {
                term1Map = new HashMap<String, Evidence>();
                map.put(term1, term1Map);
            }

            // Get the evidence instance between term1 and term2.
            Evidence evidence = term1Map.get(term2);
            if (evidence == null) {
                evidence = new Evidence(
                        new CompactSparseVector(), numSimilarityScores);
                term1Map.put(term2, evidence);
            }

            // Add the observation of this connecting dependency path.
            evidence.vector.add(basis.getDimension(path), 1);
        }

        /**
         * Returns true if this {@link EvidenceMap} has an {@link Evidence}
         * mapping between {@code term1} and {@code term2}.
         */
        public boolean contains(String term1, String term2) {
            Map<String, Evidence> term1Map = map.get(term1);
            if (term1Map == null)
                return false;
            return term1Map.containsKey(term2);
        }

        /**
         * Computes the semantic similarity for each word pair stored in this
         * {@link EvidenceMap} based on their representations in the provided
         * {@link SemanticSpace}.  If either word is missing from the space, the
         * similarity is assumed to be 0.
         */
        public void scorePairs(int sspaceNum, SemanticSpace sspace) {
            // Iterate through each of the first occurring terms.
            for (Map.Entry<String, Map<String, Evidence>> e : map.entrySet()) {

                // Check that the first term exists in the space.
                String term1 = e.getKey();
                Vector term1Vector = sspace.getVector(term1);

                // Skip to the next term if missing.
                if (term1Vector == null)
                    continue;

                // Iterate through each of the second terms observed with the
                // first term and the evidence data collected.
                for (Map.Entry<String, Evidence> f : e.getValue().entrySet()) {
                    // Confirm that the second term exists in the space.
                    String term2 = f.getKey();
                    Vector term2Vector = sspace.getVector(term2);

                    // Skip to the next term if missing.
                    if (term2Vector == null)
                        continue;

                    // Record the similarity.
                    f.getValue().similarityScores[sspaceNum] =
                        Similarity.cosineSimilarity(term1Vector, term2Vector);
                }
            }
        }

        public Map<String, Map<String, Evidence>> map() {
            return map;
        }
    }

    /**
     * An internal struct class that maintains the of two words having a
     * hypernym relationship and a cousin relationship within the wordnet
     * hierarchy based on word co-occurrence information.
     */
    public class ClassScores {

        /**
         * The probablility that the first word is a hypernym of the second
         * word.
         */
        public double hypernymScore;

        /**
         * The probablility that the first word and second word share a cousin
         * relationship.
         */
        public double cousinScore;

        /**
         * Creates a new {@link ClassScores}.
         */
        public ClassScores(double hypernymScore, double cousinScore) {
            this.hypernymScore = hypernymScore;
            this.cousinScore = cousinScore;
        }
    }

    /**
     * A data struct for recording the co-occurrence information between two
     * words and any class labels for the two words.
     */
    public class Evidence {

        /**
         * A record of the dependency paths between two terms.
         */
        public SparseDoubleVector vector;

        /**
         * A record of the semantic similarity between two terms in a number of
         * semantic spaces.
         */
        public double[] similarityScores;

        /**
         * The {@link ClassScores} if the two words are not both in wordnet.
         */
        public ClassScores classScores;

        /**
         * Creates a new {@link Evidence} instance.
         */
        public Evidence(SparseDoubleVector vector, int numSimilarityScores) {
            this.vector = vector;
            similarityScores = new double[numSimilarityScores];
            classScores = null;
        }
    }
}