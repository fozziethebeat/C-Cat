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

import edu.ucla.sspace.index.GaussianVectorGenerator;

import edu.ucla.sspace.util.Generator;

import edu.ucla.sspace.vector.DoubleVector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * This class generates a list of similar terms for eaach word in a word space
 * based on Locality Sensitive Hashing and hamming distances.  This an
 * implementation of the algorithm specified in the following paper:
 *
 *   <li style="font-family:Garamond, Georgia, serif"> Deepak Ravichandran,
 *   Patrick Pantel, and Eduard Hovy, "Randomized algorithms and NLP: using
 *   locality sensitive hash functions for high speed noun clustering,"
 *   <i>Proceedings of the 43rd Annual Meeting on Association for Computational
 *   Linguistics</i>, 43, pages 662-629, 2005,  Available <a
 *   href="http://portal.acm.org/citation.cfm?id=1219917#">here</a>
 *   </li>
 *
 * In short, this algorithm performs the following steps:
 *
 * <ol>
 *   </li> Select d random basis vectors of length k, where k is the
 *   number of features in a word space.
 *   </li> Covert each vector, v, in the word space into a d dimensional vector,
 *   with each dimension being 1 if cosine_sim(v, basis_vector(d_i)) >= 0, and 0
 *   otherwise.
 *   </li> Generate q permutation functions, which will shuffle the d dimensions
 *   in each reduced semantic vector.
 *   </li> For each permutation function pi:
 *     <ul>
 *       </li> permute all the reduced semantic vectors
 *       </li> sort them based on the lexicographical representation of the
 *             reduced bit stream
 *       </li> for each vector, v, compute the hamming distance with the B
 *             vectors before and after v in the sorted list.
 *       </li> Emit vectors that have a hamming distance below threshold t with
 *             v as nearest neighbors of v.
 *    </ul>
 * </ul>
 *
 * @author Keith Stevens
 */
public class LocalitySensitiveSimilarityListGenerator {

  /**
   *The logger for this class.
   */
  private static final Log LOG =
    LogFactory.getLog(LocalitySensitiveSimilarityListGenerator.class);

  /** 
   * A random large prime number.
   */
  private final static int PRIME = 2327257;

  /**
   * Returns a mapping for each term in the word space to it's set of most
   * similar neighbors, based on it's locality sensitive hash value.  
   *
   * @param sspace The {@link SemanticSpace} from which similarity values are
   *        extracted.
   * @param
   */
  public static Map<String, Set<String>> generateSimilarityLists(
      SemanticSpace sspace,
      int numBasisVectors,
      int numPermutations,
      int numNeighbors,
      double threshold) {
    Map<String, Set<String>> similarityLists = 
      new HashMap<String, Set<String>>();

    LOG.info("Generating basis vectors");
    // Generate the random basis vectors.
    DoubleVector[] randomBasisVectors = new DoubleVector[numBasisVectors];
    Generator<DoubleVector> basisGenerator =
      new GaussianVectorGenerator(sspace.getVectorLength());
    for (int i = 0; i < numBasisVectors; ++i)
      randomBasisVectors[i] = basisGenerator.generate();

    // Compute the distance of each word's vector from the basis vector.
    Set<String> words = sspace.getWords();
    
    LOG.info("Reducing the vectors into the basis vectors");
    // Store the reduced vectors as bit sets since all values will be binary and
    // we may have several hunderd, or even a thousand basis vectors.
    BitSet[] wordDistanceFromBasisVectors = new BitSet[words.size()];
    int wordIndex = 0;
    for (String word : words) {
      wordDistanceFromBasisVectors[wordIndex] = new BitSet(numBasisVectors);
      similarityLists.put(word, new HashSet<String>());

      // Set the bits for each basis vectors that is similar to the current
      // vector.
      for (int i = 0; i < numBasisVectors; ++i) {
        double similarity = Similarity.cosineSimilarity(
            sspace.getVector(word), randomBasisVectors[i]);
        if (similarity >= 0)
          wordDistanceFromBasisVectors[wordIndex].set(i);
      }
      wordIndex++;
    }

    //  For each permutation, permute the distance of each term from the basis
    //  vectors, sort the list of distances lexicographically, and then compute
    //  the hamming distance for each term with the B nearest terms in the
    //  sorted list.
    for (int p = 0; p < numPermutations; ++p) {
      LOG.info("Computing one permutation of the reduced vectors");
      // An array of permuted distances and the original terms.
      PermutedTerm[] permutedDistances = new PermutedTerm[words.size()];

      // Compute a new permutation function.
      Integer[] reordering = new Integer[numBasisVectors];
      for (int i = 0; i < numBasisVectors; ++i)
        reordering[i] = i;
      List<Integer> orderingList = Arrays.asList(reordering);
      Collections.shuffle(orderingList);

      int[] mapping = new int[numBasisVectors];
      for (int i = 0; i < numBasisVectors; ++i)
        mapping[i] = reordering[i].intValue();

      // Permute each distance.
      int w = 0;
      for (String word : words) {
        BitSet permutedSet = new BitSet(numBasisVectors);
        BitSet original = wordDistanceFromBasisVectors[w];
        for (int setBitIndex = original.nextSetBit(0); setBitIndex >= 0;
             setBitIndex = original.nextSetBit(setBitIndex+1))
          permutedSet.set(mapping[setBitIndex]);

        permutedDistances[w] = new PermutedTerm(permutedSet, word);
        w++;
      }

      LOG.info("Permuting the sorted vectors");
      Arrays.sort(permutedDistances);

      LOG.info("Finding the hamming distances for all of the " +
               "sorted permuted vectors");
      // Check the neighbors surroundeding each term in the sorted list for
      // other terms that have a small hamming distance.  Any term whose hamming
      // distance is below the threshold is added to the base term's similarity
      // list.
      for (int j = 0; j < words.size(); ++j) {
        // Check the hamming distance for the numNeighbors behind the current
        // term.
        for (int b = j; b > 0 && j - b < numNeighbors; b--) {
          if (hammingDistance(permutedDistances[b].distance,
                              permutedDistances[j].distance) < threshold)
            similarityLists.get(permutedDistances[j].term).add(
                permutedDistances[b].term);
        }

        // Check the hamming distance for the numNeighbors ahead of the current
        // term.
        for (int b = j; b < words.size() && b - j < numNeighbors; b++) {
          if (hammingDistance(permutedDistances[b].distance,
                              permutedDistances[j].distance) < threshold)
            similarityLists.get(permutedDistances[j].term).add(
                permutedDistances[b].term);
        }
      }
    }

    return similarityLists;
  }

  /**
   * Computes the hamming distance between two {@link BitSet}s, which is simply
   * the number of bits which differ in the two sets.
   */
  private static int hammingDistance(BitSet set1, BitSet set2) {
    int distance = 0;
    for (int i = 0; i < set1.size(); ++i)
      distance += (set1.get(i) != set2.get(i)) ? 1 : 0;
    return distance;
  }

  /**
   * A private struct used for representing a word's distance from a basis
   * vector.  Array's of this struct are ordered lexicographically.
   */
  private static class PermutedTerm implements Comparable{
    BitSet distance;
    String term;

    /**
     * Constructs a new {@link PermutedTerm} from a BitSet and a term.
     */
    public PermutedTerm(BitSet distance, String term) {
      this.distance = distance;
      this.term = term;
    }

    /**
     * Returns the string representation of this distance compared to the string
     * representation of the other distance.
     */
    public int compareTo(Object o) {
      PermutedTerm other = (PermutedTerm) o;
      return distance.toString().compareTo(other.distance.toString());
    }
  }
}
