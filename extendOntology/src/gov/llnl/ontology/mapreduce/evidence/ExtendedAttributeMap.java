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

package gov.llnl.ontology.evidence;

import java.io.IOException;
import java.io.Serializable;

import java.util.Map;
import java.util.HashMap;


/**
 * This subclass of the {@link AttributeMap} projects the dependency path
 * features into a set of replicated buckets.  A dependency path count will be
 * mapped to the index for that path in the smallest index b such that 2^b <
 * path count.
 *
 * @author Keith Stevens
 */
public class ExtendedAttributeMap extends AttributeMap {

  private static final long serialVersionUID = 1l;

  /**
   * The number of buckets to use.
   */
  private int numBuckets;

  /**
   * Constructs a new {@link ExtendedAttributeMap}.
   */
  public ExtendedAttributeMap(String[] extraFeatureColumns,
                              Map<String, Integer> dependencyPathCounts,
                              int pathCountThreshold,
                              int numBuckets) 
      throws IOException {
    super(extraFeatureColumns, dependencyPathCounts, pathCountThreshold);
    this.numBuckets = numBuckets;
  }

  /**
   * {@inheritDoc}
   */
  public int getDependencyFeatureIndex(String featureLabel, double count) {
    int logCount = (int) Math.floor(Math.log(count)/Math.log(2));
    logCount = (logCount >= numBuckets) ? numBuckets - 1 : logCount;
    return super.getDependencyFeatureIndex(
        featureLabel + "-" + logCount, count);
  }

  /**
   * {@inheritDoc}
   */
  protected void expandAttributes(Map<String, Integer> featureMap) {
    int numPaths = featureMap.size();
    Map<String, Integer> originalMap = new HashMap<String, Integer>(featureMap);
    for (Map.Entry<String, Integer> entry : originalMap.entrySet()) {
      for (int i = 1; i < numBuckets; ++i)
        featureMap.put(entry.getKey() + "-" + i, entry.getValue() + i*numPaths);
    }
  }
}
