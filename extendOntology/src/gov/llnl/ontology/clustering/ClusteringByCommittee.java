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

import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.Similarity.SimType;

import edu.ucla.sspace.clustering.Assignment;
import edu.ucla.sspace.clustering.Clustering;
import edu.ucla.sspace.clustering.HardAssignment;
import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering;
import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering.ClusterLinkage;

import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.SparseMatrix;

import edu.ucla.sspace.util.BoundedSortedMultiMap;
import edu.ucla.sspace.util.MultiMap;
import edu.ucla.sspace.util.WorkerThread;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.Vectors;
import edu.ucla.sspace.vector.VectorMath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import java.util.logging.Logger;


public class ClusteringByCommittee implements Clustering {

  public static final String PROPERTY_PREFIX =
    "gov.llnl.ontology.clustering.ClusteringByCommittee";

  public static final String THRESHOLD1_PROPERTY =
    PROPERTY_PREFIX + ".threshold1";

  public static final String THRESHOLD2_PROPERTY =
    PROPERTY_PREFIX + ".threshold2";

  public static final String THRESHOLD3_PROPERTY =
    PROPERTY_PREFIX + ".threshold3";

  public static final String HARD_CLUSTERING_PROPERTY =
    PROPERTY_PREFIX + ".useHardClustering";

  public static final String DEFAULT_THRESHOLD1 = ".50";

  public static final String DEFAULT_THRESHOLD2 = ".35";

  public static final String DEFAULT_THRESHOLD3 = ".25";

  private static final Logger LOG = Logger.getLogger(
      ClusteringByCommittee.class.getName());

  public static List<String> terms;
  public Assignment[] cluster(Matrix m, Properties props) {
    double threshold1 = Double.parseDouble(props.getProperty(
          THRESHOLD1_PROPERTY, DEFAULT_THRESHOLD1));
    double threshold2 = Double.parseDouble(props.getProperty(
          THRESHOLD2_PROPERTY, DEFAULT_THRESHOLD2));
    double threshold3 = Double.parseDouble(props.getProperty(
          THRESHOLD3_PROPERTY, DEFAULT_THRESHOLD3));
    boolean useHardClustering =
      props.getProperty(HARD_CLUSTERING_PROPERTY) != null;
    return cluster(m, threshold1, threshold2, threshold3, useHardClustering);
  }

  public Assignment[] cluster(Matrix m, int numClusters, Properties props) {
    return null;
  }

  public static Assignment[] cluster(Matrix m,
                                     double threshold1, 
                                     double threshold2, 
                                     double threshold3,
                                     boolean useHardClustering) {
    LOG.info("Starting Clustering By Committee");
    // Convert the matrix to a sparse matrix.
    if (!(m instanceof SparseMatrix))
      throw new IllegalArgumentException("CBC only accepts sparse matrices");

    final SparseMatrix sm = (SparseMatrix) m;

    LOG.info("Mapping columns to rows with non zero values for that column");
    // Initialize a list which will contain the set of rows that have a non zero
    // value for a given feature.
    final List<Set<Integer>> featureOccurrenceSets = 
      new ArrayList<Set<Integer>>();
    for (int i = 0; i < m.columns(); ++i)
      featureOccurrenceSets.add(new HashSet<Integer>());

    // Compue the set of rows that have non zero values for each feature.
    for (int r = 0; r < m.rows(); ++r)
      for (int index : sm.getRowVector(r).getNonZeroIndices())
        featureOccurrenceSets.get(index).add(r);

    LOG.info("Finding candidate committees");
    // Setup concurrent data structures so that the similarity lists are
    // generated in parralel.
    final BlockingQueue<Runnable> workQueue =
      new LinkedBlockingQueue<Runnable>();
    for (int i = 0; i < Runtime.getRuntime().availableProcessors(); ++i) {
      Thread t = new WorkerThread(workQueue);
      t.start();
    }
    final Semaphore rowsProcessed = new Semaphore(0);

    // Phase 1 of the Clustering By Committee algorithm.  For each data point,
    // find the k nearest neighbors.  These will serve as the basis for
    // potential committees in Phase 2.
    final List<DataPoint> rowSimilarityLists = new ArrayList<DataPoint>();
    for (int r = 0; r < m.rows(); ++r) {
      final int rowIndex = r;
      workQueue.offer(new Runnable() {
        public void run() {
          DataPoint dataPoint = buildSimilarityListForRow(
            sm, rowIndex, featureOccurrenceSets);
          synchronized (rowSimilarityLists) {
            rowSimilarityLists.add(dataPoint);
          }
          rowsProcessed.release();
        }
      });
    }

    try {
      rowsProcessed.acquire(m.rows());
    } catch (InterruptedException ie) {
      throw new Error("Interrupted while building row similarity lists", ie);
    }

    LOG.info("Building the list of committees");
    // Phase 2, compute the set of committes.
    List<CandidateCommittee> committees = buildCommittees(
        workQueue, rowSimilarityLists, sm, threshold1, threshold2, threshold3);

    LOG.info("Making the cluster assignments for each data point");
    Assignment[] assignments = new Assignment[sm.rows()];
    if (useHardClustering) {
      for (int r = 0; r < sm.rows(); ++r) {
        SparseDoubleVector v = sm.getRowVector(r);

        // Find the committee that is most similar to the current data point.
        int i = 0;
        double bestScore = 0;
        int centroidIndex = 0;
        for (CandidateCommittee committee : committees) {
          double sim = Similarity.cosineSimilarity(committee.centroid(), v);
          if (sim > bestScore) {
            bestScore = sim;
            centroidIndex = i;
          }
          i++;
        }

        // Make the assignment.
        assignments[r] = new HardAssignment(centroidIndex);
      }
    } else {
      for (int r = 0; r < sm.rows(); ++r) {
        SparseDoubleVector v = sm.getRowVector(r);

        // Create a sorted descending list of the committees based on their
        // similarity with the given data point.
        Set<CandidateCommittee> bestCommittees =
          new TreeSet<CandidateCommittee>();
        int committeeIndex = 0;
        for (CandidateCommittee committee : committees)
          bestCommittees.add(new CandidateCommittee(
                null, committeeIndex++, null, 
                committee.centroid(),
                Similarity.cosineSimilarity(committee.centroid(), v)));
        
        // For the first 200 committees, select the committees that are distinct
        // from any of the previously chosen committees, starting with no
        // selected committees
        List<Integer> selectedCommittees = new ArrayList<Integer>();
        int count = 0;
        for (CandidateCommittee committee : bestCommittees) {
          // Break if we have evaluated 200 committees or the similarity score
          // drops too low.
          if (count >= 200 || committee.score() < .01)
            break;

          // Assign the data point to the committee if the committee is
          // significantly distinct from all other assigned committees.  After
          // assigning a data point to a committee, remove from the data point
          // any shared features.
          if (committeeIsDistinct(committee, selectedCommittees, committees)) {
            selectedCommittees.add(committee.index());

            // Remove any overlapping features.
            v = (SparseDoubleVector) Vectors.copyOf(v);
            for (int index : v.getNonZeroIndices())
              if (committee.centroid().get(index) == 0d)
                v.set(index, 0);
          }

          // Create an Assignment based on the selected committees.
          int[] manyAssignments = new int[selectedCommittees.size()];
          count = 0;
          for (Integer cluster : selectedCommittees)
            manyAssignments[count++] = cluster;
          assignments[r] = new SoftAssignment(manyAssignments);
        }
      }
    }
    return assignments;
  }

  public static DataPoint buildSimilarityListForRow(
      SparseMatrix sm,
      int r,
      List<Set<Integer>> featureOccurrenceSets) {
    // Compute the top N features for the given row.
    MultiMap<Double, Integer> sortedFeatureMap = new
      BoundedSortedMultiMap<Double, Integer>(100);
    SparseDoubleVector rowVector = sm.getRowVector(r);
    for (int index : rowVector.getNonZeroIndices())
      sortedFeatureMap.put(rowVector.get(index), index);

    // For each of the top N features, get the set of rows that share the
    // feature.  Combine all these row sets into a single set.
    Set<Integer> rowsWithSharedFeature = new HashSet<Integer>();
    for (Map.Entry<Double, Integer> validFeature :
         sortedFeatureMap.entrySet())
      rowsWithSharedFeature.addAll(
          featureOccurrenceSets.get(validFeature.getValue()));

    // Of the data points that share one of the top N features, compute the k
    // nearest neighbors based on cosine similarity.
    MultiMap<Double, Integer> nearestNeighbors = new
      BoundedSortedMultiMap<Double, Integer>(20);
    for (Integer dataPoint : rowsWithSharedFeature)
      nearestNeighbors.put(
          Similarity.cosineSimilarity(rowVector, sm.getRowVector(dataPoint)),
          dataPoint);

    // Save the nearest neighbors in the similarity lists.
    Set<Integer> neighbors = new HashSet<Integer>();
    System.out.println("neighbors for: " + terms.get(r));
    for (Map.Entry<Double, Integer> neighbor : nearestNeighbors.entrySet()) {
      neighbors.add(neighbor.getValue());
      System.out.println(terms.get(neighbor.getValue()));
    }

    return new DataPoint(r, neighbors);
  }

  /**
   * Returns true if the given committee is significantlly different from all
   * of the committees specified in {@code selectedCommittess}, the list of
   * previously selected committees for a data point.
   */
  private static boolean committeeIsDistinct(
      CandidateCommittee committee,
      List<Integer> selectedCommittees,
      List<CandidateCommittee> committees) {
    for (Integer index : selectedCommittees)
      if (Similarity.cosineSimilarity(
            committee.centroid(), committees.get(index).centroid()) > .10)
        return false;
    return true;
  }

  /**
   * Recursively finds centroids that are highly distinct and composed of a few
   * highly similar data points, which are deemed committees.  Based on the
   * nearest neighbors for each data point given, a set of clusters will be
   * generated for each list of neighbors and the cluster with a high
   * intersimilarity score will be considered a candidate.  Of all the candidate
   * committees, only committees that are highly distinct will be maintained.
   * If any data points are significantly dissimilar to any of the found
   * committees, they will be used as the basis for finding new committees.
   */
  private static List<CandidateCommittee> buildCommittees(
      BlockingQueue<Runnable> workQueue,
      List<DataPoint> rowSimilarityLists,
      final SparseMatrix sm,
      final double threshold1,
      double threshold2,
      double threshold3) {

    // From the list of row similarities, extract one candidate committee from
    // each list.  A candidate committee will be the highest scoring cluster
    // after performing HAC on the nearest neighbors for a given row.
    final Set<CandidateCommittee> candidates =
      new TreeSet<CandidateCommittee>();

    final Semaphore dataPointsProcessed = new Semaphore(0);
    for (DataPoint dataPoint : rowSimilarityLists) {
      final DataPoint d = dataPoint;
      workQueue.offer(new Runnable() {
        public void run() {
          CandidateCommittee committee = buildCommitteeForRow(
            d, sm, threshold1);

          synchronized (candidates) {
            candidates.add(committee);
          }
          dataPointsProcessed.release();
        }
      });
    }

    try {
      dataPointsProcessed.acquire(rowSimilarityLists.size());
    } catch (InterruptedException ie) {
      throw new Error("Interrupted while building candidate committees", ie);
    }

    // Compute the list of valid committee members from the candidates.
    List<CandidateCommittee> committees = new ArrayList<CandidateCommittee>();
    for (CandidateCommittee candidate : candidates)
      addToCommitteeIfDistinct(committees, candidate, threshold2);

    // If there were no valid candidates, then we are done.
    if (committees.size() == 0)
      return committees;

    // For each data point, add it to a list of residuals if it is less
    // similar to all committees than some threshold.
    List<DataPoint> residuals = new ArrayList<DataPoint>();
    for (DataPoint dataPoint : rowSimilarityLists) {
      SparseDoubleVector v = sm.getRowVector(dataPoint.row());
      for (CandidateCommittee committee : committees)
        if (Similarity.cosineSimilarity(v, committee.centroid()) < threshold3)
          residuals.add(dataPoint);
    }

    // Find any committees from the list of residuals.
    committees.addAll(buildCommittees(
          workQueue, residuals, sm, threshold1, threshold2, threshold3));
    return committees;
  }

  /**
   * If the given candidate is significantly dissimilar to all known committees,
   * add it to the list of committees and remove the committee members from it's
   * source neighbor list.
   */
  private static void addToCommitteeIfDistinct(
      List<CandidateCommittee> committees,
      CandidateCommittee candidate,
      double threshold) {
    for (CandidateCommittee validCommittee : committees) {
      if (Similarity.cosineSimilarity(
            candidate.centroid(), validCommittee.centroid()) >= threshold)
        return;
    }
    committees.add(candidate);
    candidate.removeFromNeighborList();
  }

  public static CandidateCommittee buildCommitteeForRow(DataPoint dataPoint,
                                                        SparseMatrix sm, 
                                                        double threshold1) {
      // Convert the nearest neighbors to a matrix and cluster them using HAC
      // wth the mean link critera.
      List<SparseDoubleVector> v = new ArrayList<SparseDoubleVector>();
      for (Integer neighbor : dataPoint.neighbors())
        v.add(sm.getRowVector(neighbor));
      int[] assignments = HierarchicalAgglomerativeClustering.clusterRows(
          Matrices.asSparseMatrix(v), threshold1, 
          ClusterLinkage.MEAN_LINKAGE, SimType.COSINE);

      // Compute the cluster size, cluster score, and centroid for each of the
      // discovered clusters.
      List<Integer> clusterSizes = new ArrayList<Integer>();
      List<Double> clusterScores = new ArrayList<Double>();
      List<SparseDoubleVector> centroids = new ArrayList<SparseDoubleVector>();
      int i = 0;
      for (Integer neighbor : dataPoint.neighbors()) {
        int assignment = assignments[i++];

        // If the assignment has not been observed yet, expand the set of known
        // cluster values to include at least the current assignment.
        while (assignment >= clusterSizes.size()) {
          clusterSizes.add(0);
          clusterScores.add(0d);
          centroids.add(new CompactSparseVector(sm.columns()));
        }

        // Increment the size of the cluster.
        clusterSizes.set(assignment, clusterSizes.get(assignment) + 1);

        // Compute the similarity between the current neighbor and the current
        // centroid.  Due to the cosine similarity metric we can compute the
        // pairwis similarity of the cluster simply by iteratively building up
        // the centroid and comparing it to the neighbor about to be added to
        // it.
        SparseDoubleVector centroid = centroids.get(assignment);
        SparseDoubleVector rowVector = sm.getRowVector(neighbor);
        double sim = Similarity.cosineSimilarity(centroid, rowVector);
        clusterScores.set(assignment, clusterScores.get(assignment) + sim);
        VectorMath.add(centroid, sm.getRowVector(neighbor));
      }

      // The similarity score is computed as |clusterSize| * avgsim(cluster).
      // But since we compute the average similarity by through an iterative
      // method, this reduces to the cluster score summation already computed.

      // Find the cluster with best score.
      double bestScore = 0;
      int bestIndex = 0;
      i = 0;
      for (Double score : clusterScores) {
        if (score > bestScore) {
          bestScore = score;
          bestIndex = i;
        }
        i++;
      }

      // Add the highest scoring cluster as a candidate committee.  Keep
      // track of the neighbor list, the cluster index of the best cluster,
      // and the assignments so that  if this committee is selected, we can
      // remove the vectors that compose it from the neighbor list.
      return new CandidateCommittee(
          dataPoint.neighbors(), bestIndex, assignments,
          centroids.get(bestIndex), bestScore);
    }

  /**
   * A simple struct that tracks the neighbors for a given row and the row
   * index.
   */
  private static class DataPoint {
    private Set<Integer> neighbors;
    private int rowIndex;

    /**
     * Constructs a new {@link DataPoint}
     */
    public DataPoint(int rowIndex, Set<Integer> neighbors) {
      this.rowIndex = rowIndex;
      this.neighbors = neighbors;
    }

    /**
     * Returns the set of neighbors.
     */
    public Set<Integer> neighbors() {
      return neighbors;
    }

    /**
     * Returns the row index for this {@link DataPoint}.
     */
    public int row() {
      return rowIndex;
    }
  }

  /**
   * A simple struct for representing a Committee.  This class implements {@link
   * Comparable} so that an ordered, descending list of Committees can be made
   * based on the score.
   */
  private static class CandidateCommittee implements Comparable {

    /**
     * The set of neighbors that this committee came from.
     */
    private Set<Integer> neighbors;

    /**
     * The cluster index for this committee, based on the neighbor list used to
     * generate the committee.
     */
    private int candidateIndex;

    /**
     * The assignments for each of the {@code neighbors}.
     */
    private int[] assignments;

    /**
     * The centroid for this committee.
     */
    private SparseDoubleVector centroid;

    /**
     * The score for this committee.
     */
    private double score;

    /**
     * Constructs a new {@link CandidateCommittee}.
     */
    public CandidateCommittee(Set<Integer> neighbors,
                              int candidateIndex,
                              int[] assignments,
                              SparseDoubleVector centroid,
                              double score) {
      this.neighbors = neighbors;
      this.candidateIndex = candidateIndex;
      this.assignments = assignments;
      this.centroid = centroid;
      this.score = score;
    }

    /**
     * Returns the centroid.
     */
    public SparseDoubleVector centroid() {
      return centroid;
    }

    public int index() {
      return candidateIndex;
    }

    /**
     * Removes the neighbors that compose this committee from the source set of
     * neighbors.
     */
    public void removeFromNeighborList() {
      Set<Integer> toRemove = new HashSet<Integer>();
      int i = 0;
      for (Integer neighbor : neighbors) 
        if (assignments[i++] == candidateIndex)
          toRemove.add(neighbor);
      for (Integer neighborIndex : toRemove)
        neighbors.remove(neighborIndex);
    }

    /**
     * Returns the differents between another Committee's score and this
     * Committee's score.
     */
    public int compareTo(Object o) {
      CandidateCommittee other = (CandidateCommittee) o;
      return Double.compare(other.score, this.score);
    }

    public double score() {
      return score;
    }
  }
}
