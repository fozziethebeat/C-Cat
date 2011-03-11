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
        ExtendWordNet builder = new ExtendWordNet(
                new CoNLLDependencyExtractor(),
                new RelationPathBasisMapping(),
                new ConjunctionTransform(),
                args.length - 1);
        Iterator<Document> corpusIter = new UkWacDependencyFileIterator(
                args[0]);
        while (corpusIter.hasNext())
            builder.gatherEvidence(corpusIter.next().reader());

        for (int s = 1; s < args.length; ++s) {
            SemanticSpace sspace = new StaticSemanticSpace(args[s]);
            builder.applySimilarityScores(s-1, sspace);
        }

        builder.trainModel(5);

        builder.labelUnknownEvidence();

        builder.extendWordNet();
    }

    public void labelUnknownEvidence() {
    }

    public void extendWordNet() {
    }

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

    public void applySimilarityScores(int sspaceNum, SemanticSpace sspace) {
        knownPositives.scorePairs(sspaceNum, sspace);
        knownNegatives.scorePairs(sspaceNum, sspace);
        unknownEvidence.scorePairs(sspaceNum, sspace);
    }

    public boolean trainModel(int numPasses) {
        AdaptiveLogisticRegression model = new AdaptiveLogisticRegression(
                basis.numDimensions(), 2, new L1());
        for (int i = 0; i < numPasses; ++i) {
            trainHypernyms(knownPositives.map(), model, 1);
            trainHypernyms(knownNegatives.map(), model, 0);
        }

        State<AdaptiveLogisticRegression.Wrapper> best = model.getBest();
        if (best == null)
            return false;

        hypernymPredictor = best.getPayload().getLearner().getModels().get(0);

        model = new AdaptiveLogisticRegression(
                numSimilarityScores, 2, new L1());
        for (int i = 0; i < numPasses; ++i) {
            trainCousins(knownPositives.map(), model);
            trainCousins(knownNegatives.map(), model);
        }

        best = model.getBest();
        if (best == null)
            return false;

        cousinPredictor = best.getPayload().getLearner().getModels().get(0);
        return true;
    }

    public void gatherEvidence(BufferedReader document) {
        try {
            for (DependencyTreeNode[] tree = null;
                    (tree = extractor.readNextTree(document)) != null; ) {
                tree = transformer.transform(tree);

                for (DependencyTreeNode treeNode : tree) {
                    // Reject any nodes that are not nouns.
                    if (!treeNode.pos().startsWith("NN"))
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
                        if (!path.last().pos().startsWith("NN"))
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

    private void addTermEvidence(String firstTerm,
                                 String secondTerm,
                                 DependencyPath path) {
        if (knownPositives.contains(firstTerm, secondTerm))
            knownPositives.add(firstTerm, secondTerm, path);
        else if (knownNegatives.contains(firstTerm, secondTerm))
            knownNegatives.add(firstTerm, secondTerm, path);
        else if (unknownEvidence.contains(firstTerm, secondTerm))
            unknownEvidence.add(firstTerm, secondTerm, path);
        else {
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

    private void trainHypernyms(Map<String, Map<String, Evidence>> map,
                                OnlineLearner model,
                                int classLabel) {
        for (Map<String, Evidence> value : map.values())
            for (Evidence e : value.values()) 
                model.train(classLabel, new MahoutSparseVector(e.vector));
    }

    private void trainCousins(Map<String, Map<String, Evidence>> map,
                              OnlineLearner model) {
        for (Map.Entry<String, Map<String, Evidence>> entry: map.entrySet()) {
            for (Map.Entry<String, Evidence> ev : entry.getValue().entrySet()) {
                // Determine whether or not the two terms are cousins.
                // Initially assume that they arent.
                Pair<Integer> cousinDepth = SynsetRelations.getCousinDistance(
                        entry.getKey(), ev.getKey(), 7);
                int classLabel = 0;
                if (cousinDepth.x != Integer.MAX_VALUE &&
                    cousinDepth.y != Integer.MAX_VALUE)
                    classLabel = 1;

                model.train(classLabel,
                            new DenseVector(ev.getValue().similarityScores));
            }
        }
    }

    private class EvidenceMap {

        private final HashMap<String, Map<String, Evidence>> map;

        private final DependencyPathBasisMapping basis;

        public EvidenceMap(DependencyPathBasisMapping basis) {
            this.basis = basis;

            map = new HashMap<String, Map<String, Evidence>>();
        }

        public void add(String term1, String term2, DependencyPath path) {
            Map<String, Evidence> term1Map = map.get(term1);
            if (term1Map == null) {
                term1Map = new HashMap<String, Evidence>();
                map.put(term1, term1Map);
            }

            Evidence evidence = term1Map.get(term2);
            if (evidence == null) {
                evidence = new Evidence(
                        new CompactSparseVector(), numSimilarityScores);
                term1Map.put(term2, evidence);
            }

            evidence.vector.add(basis.getDimension(path), 1);
        }

        public boolean contains(String term1, String term2) {
            Map<String, Evidence> term1Map = map.get(term1);
            if (term1Map == null)
                return false;
            return term1Map.containsKey(term2);
        }

        public void scorePairs(int sspaceNum, SemanticSpace sspace) {
            for (Map.Entry<String, Map<String, Evidence>> e : map.entrySet()) {
                String term1 = e.getKey();
                Vector term1Vector = sspace.getVector(term1);

                if (term1Vector == null)
                    continue;

                for (Map.Entry<String, Evidence> f : e.getValue().entrySet()) {
                    String term2 = f.getKey();
                    Vector term2Vector = sspace.getVector(term2);

                    if (term2Vector == null)
                        continue;

                    f.getValue().similarityScores[sspaceNum] =
                        Similarity.cosineSimilarity(term1Vector, term2Vector);
                }
            }
        }

        public Map<String, Map<String, Evidence>> map() {
            return map;
        }
    }

    public class ClassScores {
        public double hyponymScore;
        public double[] cousinScores;

        public ClassScores(double hyponymScore, double[] cousinScores) {
            this.hyponymScore = hyponymScore;
            this.cousinScores = cousinScores;
        }
    }

    public class Evidence {
        public SparseDoubleVector vector;
        public double[] similarityScores;

        public Evidence(SparseDoubleVector vector, int numSimilarityScores) {
            this.vector = vector;
            similarityScores = new double[numSimilarityScores];
        }
    }
}
