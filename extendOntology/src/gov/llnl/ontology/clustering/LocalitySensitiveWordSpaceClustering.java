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

package gov.llnl.ontology.clustering;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.StaticSemanticSpace;

import edu.ucla.sspace.util.SerializableUtil;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.Vectors;
import edu.ucla.sspace.vector.VectorMath;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOError;
import java.io.IOException;
import java.io.File;
import java.io.Serializable;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * This class creates a soft cluster for a subset of the words in a word space
 * based on the similarity lists generated.  This class can be serialized and
 * deserialized easily so that other codes can retrieve the similarity values.
 *
 * @author Keith Stevens
 */
public class LocalitySensitiveWordSpaceClustering
    implements ClusterSimilarity, Serializable {

  private static final long serialVersionUID = 1l;

  /**
   *The logger for this class.
   */
  private static final Log LOG =
    LogFactory.getLog(LocalitySensitiveWordSpaceClustering.class);

  /**
   * A mapping from terms to assigned cluster indices.
   */
  private final Map<String, Set<Integer>> clusterAssignments;

  /**
   * The set of terms in each cluster.
   */
  private final List<Set<String>> clusters;

  /**
   * The centroid vector for each cluster.
   */
  private final DoubleVector[] clusterCentroids;

  /**
   * A mapping from words to their semantic vectors.  This is a subset of the
   * {@link SemanticSpace} used to generate the clusters.
   */
  private final Map<String, DoubleVector> wordSpace;

  /**
   * The text descrbibing the {@link SemanticSpace} algorithm used to generate
   * this clustering assignment.
   */
  private final String sspaceName;

  public static void main(String[] args) {
    ArgOptions options = new ArgOptions();
    options.addOption('c', "numClusters",
                      "Set the number of clusters, i.e., similarity lists, " +
                      "that should be maintained (default 10000)",
                      true, "INT", "Optional");
    options.addOption('b', "numBasisVectors",
                      "Set the number of basis vectors, i.e., the number of " +
                      "bits used to represent each semantic vector " +
                      "(default 3000)", true, "INT", "Optional");
    options.addOption('p', "numPermutations",
                      "Set the number of permutations used (default: 1000)",
                      true, "INT", "Optional");
    options.addOption('s', "beamSearchSize",
                      "Set the size of the beam search, i.e., how many " +
                      "neighbors before and after each vector have their " +
                      "hamming distance computed (default: 100)",
                      true, "INT", "Optional");
    options.addOption('t', "threshold",
                      "Set the minimum accepted hamming distance " +
                      "(default: .15)",
                      true, "DOUBLE", "Optional");
    options.parseOptions(args);

    if (options.numPositionalArgs() != 2)
      throw new IllegalArgumentException(
          "usage: java LocalitySensitiveWordSpaceClustering [options] " +
          "sspace.bin clusterSim.out\n" + 
          options.prettyPrint());
    try {
      SemanticSpace sspace = new StaticSemanticSpace(
          options.getPositionalArg(0));

      ClusterSimilarity cs = new LocalitySensitiveWordSpaceClustering(
          sspace,
          options.getIntOption('c', 10000), 
          options.getIntOption('b', 3000), 
          options.getIntOption('p', 1000), 
          options.getIntOption('s', 100), 
          options.getDoubleOption('t', .15)); 
      SerializableUtil.save(cs, new File(options.getPositionalArg(1)));
    } catch (IOException ioe) {
      throw new IOError(ioe);
    }
  }

  /**
   * Creates a new {@link LocalitySensitiveWordSpaceClustering} by generating
   * similarity lists with a {@link LocalitySensitiveSimilarityListGenerator}
   * and then saving only a small portion of those similarity lists.  Similarity
   * lists that are maintained become clusters, and each word in the remaining
   * word space can be placed in several clusters.
   */
  public LocalitySensitiveWordSpaceClustering(SemanticSpace sspace, 
                                              int numClusters,
                                              int numBasisVectors,
                                              int numPermutations,
                                              int numNeighbors,
                                              double threshold) {
    sspaceName = sspace.getSpaceName();

    LOG.info("Creating the similarity lists.");
    // Compute the similarity lists for each word.
    Map<String, Set<String>> similarityLists = 
      LocalitySensitiveSimilarityListGenerator.generateSimilarityLists(
          sspace, numBasisVectors, numPermutations, numNeighbors, threshold);

    // Convert the key list to a List so that it can be shuffled.
    List<String> keyList = new ArrayList<String>(similarityLists.size());
    for (String key : similarityLists.keySet())
      keyList.add(key);
    Collections.shuffle(keyList);

    // Drop all but numClusters similarity lists.  The remaining lists will
    // represent the clusters.
    for (int i = keyList.size() - 1; i >= numClusters; i--)
      similarityLists.remove(keyList.get(i));

    // Compute the centroid for each cluster and a mapping from each term to
    // it's list of possible clusters.
    clusters = new ArrayList<Set<String>>(numClusters);
    clusterCentroids = new DoubleVector[numClusters];
    clusterAssignments = new HashMap<String, Set<Integer>>();
    int clusterId = 0;

    LOG.info("Saving only the clusters for the first numClusters " +
             "similarity lists");
    // Setup the sub word space.
    wordSpace = new HashMap<String, DoubleVector>();

    for (Map.Entry<String, Set<String>> clusterEntry : 
        similarityLists.entrySet()) {
      clusterCentroids[clusterId] = new DenseVector(sspace.getVectorLength());

      // Build up the cluster centroids and make the assignments.
      for (String termInCluster : clusterEntry.getValue()) {
        // Add the term to the sub word space if it does not already exist.
        DoubleVector termVector = wordSpace.get(termInCluster);
        if (termVector == null) {
          termVector = Vectors.asDouble(sspace.getVector(termInCluster));
          wordSpace.put(termInCluster, termVector);
        }

        VectorMath.add(clusterCentroids[clusterId], termVector);
        Set<Integer> assignments = clusterAssignments.get(termInCluster);
        if (assignments == null) {
          assignments = new HashSet<Integer>();
          clusterAssignments.put(termInCluster, assignments);
        }

        assignments.add(clusterId);
      }

      // Store the terms in each cluster.
      clusters.add(clusterEntry.getValue());
      clusterId++;
    }
  }

  /**
   * Returns the similarity between {@code term1} and {@code term2}.  If both
   * terms are in the same cluster, their similarity is the minimum similarity
   * of the cosine similarity for each term to the clusters centroid, otherwise
   * it is 0.
   */
  public double getTermSimilarity(String term1, String term2) {
    Set<Integer> term1Assignments = clusterAssignments.get(term1);

    // Return 0 if term 1 is not in any cluster.
    if (term1Assignments == null)
      return 0;

    // Iterate through the clusters that term1 is assigned to to determine
    // whether or not term2 is also assigned to one.  Return the similarity for
    // term1 and term2 immediately upon finding a cluster with both terms.
    for (Integer clusterId : term1Assignments) {
      if (clusters.get(clusterId).contains(term2)) {
        double term1Sim = Similarity.cosineSimilarity(
            clusterCentroids[clusterId], wordSpace.get(term1));
        double term2Sim = Similarity.cosineSimilarity(
            clusterCentroids[clusterId], wordSpace.get(term2));
        return Math.min(term1Sim, term2Sim);
      }
    }
    return 0;
  }

  /**
   * Returns "lsh_" + the name of the sspace algorithm used to generate the
   * cluster assignments.
   */
  public String toString() {
    return "lsh_" + sspaceName;
  }
}
