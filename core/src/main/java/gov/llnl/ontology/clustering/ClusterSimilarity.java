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


/**
 * This interface provides a mechanism for determining the similarity between
 * two words after their semantic representations have been clustered.
 * Similarity between two words is based on two factors: 1) the semantic
 * representations and 2) cluster assignments.  Two terms that are assigned to
 * diffefent clusters should have a lower similarity, possibly 0, than two terms
 * assigned to the same cluster.
 *
 * </p>
 * 
 * Implementations should maintain a word to vector mapping, based on a {@link
 * SemanticSpace} algorithm, so that semantic vectors are easily retrievable.
 *
 * </p>
 *
 * Implementations should implement {@code toString} so that other codes can
 * query the label of the clustering method used.
 *
 * </p>
 *
 * Implementations should also implement {@link java.io.Serializable} so that
 * other codes can store and restore instances of this interface.
 *
 * @author Keith Stevens
 */
public interface ClusterSimilarity {

  /**
   * Returns the similarity between {@code term1} and {@code term2}.  This
   * similarity should be based on part on the clustering assignments.  
   *
   * </p>
   * A common approach is to limit similarity to be between terms in the same
   * cluster.  All other term pairings have a similarity of 0.
   */
  double getTermSimilarity(String term1, String term2);
}
