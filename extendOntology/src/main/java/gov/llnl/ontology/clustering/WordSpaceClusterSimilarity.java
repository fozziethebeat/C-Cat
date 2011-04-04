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

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.StaticSemanticSpace;

import edu.ucla.sspace.clustering.Assignment;
import edu.ucla.sspace.clustering.Assignments;
import edu.ucla.sspace.clustering.Clustering;
import edu.ucla.sspace.clustering.ClusteringByCommittee;

import edu.ucla.sspace.util.ReflectionUtil;
import edu.ucla.sspace.util.SerializableUtil;

import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class WordSpaceClusterSimilarity 
    implements ClusterSimilarity, Serializable {

  private static final long serialVersionUID = 1l;

  public static void main(String[] args) throws Exception {
    if (args.length != 3)
      throw new IllegalArgumentException(
          "usage: WordSpaceClusterSimilarity " +
          "<sspace> CLUSTER_CLASS <clusterSim>");

    // Load up the word space and clustering algorithm.
    SemanticSpace sspace = new StaticSemanticSpace(args[0]);
    Clustering clustering = ReflectionUtil.getObjectInstance(args[1]);
    // Convert the vectors in the space into a matrix.
    List<SparseDoubleVector> sspaceVectors =
      new ArrayList<SparseDoubleVector>();
    Set<String> terms = sspace.getWords();
    for (String term : terms)
      sspaceVectors.add((SparseDoubleVector) sspace.getVector(term));

    // Cluster the vectors and create a new WordSpaceClusterSimilarity.
    Assignments assignments = clustering.cluster(
        Matrices.asSparseMatrix(sspaceVectors), System.getProperties());
    ClusterSimilarity cs = new WordSpaceClusterSimilarity(sspace, assignments);

    // Save the similarity to the specified file.
    SerializableUtil.save(cs, new File(args[2]));
  }

    
  /**
   * A mapping from terms to assigned cluster indices.
   */
  private final Map<String, Assignment> clusterAssignments;

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

  public WordSpaceClusterSimilarity(SemanticSpace sspace,
                                    Assignments assignments) {
    wordSpace = new HashMap<String, DoubleVector>();
    clusterAssignments = new HashMap<String, Assignment>();
    clusters = new ArrayList<Set<String>>();
    sspaceName = sspace.getSpaceName();

    Set<String> words = sspace.getWords();
    List<DoubleVector> centroids = new ArrayList<DoubleVector>();
    int index = 0;
    for (String term : words) {
      DoubleVector termVector = Vectors.asDouble(sspace.getVector(term));
      wordSpace.put(term, termVector);
      Assignment assignment = assignments.get(index);
      clusterAssignments.put(term, assignment);

      for (int clusterIndex : assignment.assignments()) {
        while (clusterIndex >= clusters.size()) {
          clusters.add(new HashSet<String>());
          centroids.add(new CompactSparseVector(sspace.getVectorLength()));
        }

        clusters.get(clusterIndex).add(term);
        VectorMath.add(centroids.get(clusterIndex), termVector);
      }
    }
    clusterCentroids = centroids.toArray(new DoubleVector[0]);
  }

  /**
   * Returns the similarity between {@code term1} and {@code term2}.  If both
   * terms are in the same cluster, their similarity is the minimum similarity
   * of the cosine similarity for each term to the clusters centroid,
   * otherwise it is 0.
   */
  public double getTermSimilarity(String term1, String term2) {
    Assignment term1Assignments = clusterAssignments.get(term1);

    // Return 0 if term 1 is not in any cluster.
    if (term1Assignments == null)
      return 0;

    // Iterate through the clusters that term1 is assigned to to determine
    // whether or not term2 is also assigned to one.  Return the similarity
    // for term1 and term2 immediately upon finding a cluster with both terms.
    for (int clusterId : term1Assignments.assignments()) {
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
   * Returns "cluster_" + the name of the sspace algorithm used to generate
   * the cluster assignments.
   */
  public String toString() {
    return "cluster_" + sspaceName;
  }
}
