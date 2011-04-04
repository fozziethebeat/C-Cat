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

import gov.llnl.ontology.table.SchemaUtil;
import gov.llnl.ontology.table.WordNetEvidenceSchema;

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.CompactSparseVector;

import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;

import java.io.IOException;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;


/**
 * This abstract class transforms values from a row in the {@link
 * WordNetEvidenceSchema} table into an appropriate {@link DoubleVector} used by
 * .  The class value's will be projected into the integer set
 * \{0,...,num_unique_class_values\} and stored in index 0 for all instances.
 * This builder also allows a set of user specified features to be added to
 * instances, which go from index 1 to the number of user specified features.
 * Lastly, dependency features can be added to instances, and will be the last
 * set of instances.  The mapping for feature labels to their indicides is
 * provided by a {@link AttributeMap}.  Any feature label that is not stored in
 * the map is not stored as a feature.
 *
 * <p>
 *
 * This class will modify the attribute set for the class feature, stored by a
 * {@link AttributeMap} based on the possible class labels.
 *
 * @author Keith Stevens
 */
public abstract class EvidenceInstanceBuilder {

  /**
   * A mapping from feature labels to their indicese in a feature vector.
   */
  private AttributeMap attributeMap;

  /**
   * If set to true, all dependency path values will be stored as binary values.
   */
  private boolean useBinary;

  /**
   * If set to true, dependency features will be stored in the feature vectors.
   */
  private boolean useDependencyPathsAsFeatures;

  /**
   * The document source from which dependency paths should be extracted.
   */
  private String source;

  /**
   * The set of extra feature labels to use.
   */
  private String[] extraFeatureColumns;

  /**
   * Constructs a new {@link EvidenceInstanceBuilder}
   *
   * @param attributeMap The {@link AttributeMap} that specifies the index
   *        for each feature label.
   * @param extraFeatureColumns The set of extra columns that should serve as
   *        features, in the form of "family:qualifier".
   * @param useDependencyPathsAsFeatures If true, dependency features will be
   *        added to the feature vectors.
   * @param useBinaryForPathFeatures If true, dependency path counts will be
   *        turned into binary values, i.e., true for any non zero count.
   */
  public EvidenceInstanceBuilder(AttributeMap attributeMap,
                                 boolean useBinaryForPathFeatures,
                                 String source)
      throws IOException {
    this.attributeMap = attributeMap;
    this.extraFeatureColumns = attributeMap.extraFeatureColumns();
    this.useDependencyPathsAsFeatures = 
      attributeMap.useDependencyPathsAsFeatures();
    this.source = source;
  }

  /**
   * Returns a new {@link Scan} instance that will scan for the class label, any
   * extra requested features, and dependency features.
   */
  public Scan addToScan(Scan scan) {
    // Add the columns that will be used as feature values during
    // classification.
    for (String familyAndQualifier : extraFeatureColumns)
      scan.addColumn(familyAndQualifier.getBytes());

    // Add the column that will be used for determining the class of each
    // instance.
    scan.addColumn(
        WordNetEvidenceSchema.CLASS_CF.getBytes(), getClassQualifier());
    setFilter(scan);

    // Add the dependency class family.
    if (useDependencyPathsAsFeatures)
      scan.addColumn(
          WordNetEvidenceSchema.DEPENDENCY_FEATURE_CF.getBytes(),
          source.getBytes());

    return scan;
  }

  /**
   * Returns a new {@link DoubleVector} feature vector based on the {@link
   * Result} {@code resultRow}.  If the row lacks any of the required features,
   * this returns {@code null}.  Otherwise it returns a {@link
   * SparseDoubleVector} that has the class value as the first value, and a set
   * of other values whose indicies are specified by a {@link AttributeMap}.
   * No values are left
   * missing.
   */
  public DoubleVector getInstanceFrom(Result resultRow) {
    SparseDoubleVector dataPoint = new CompactSparseVector(
        attributeMap.attributeVector().length);

    // Add the feature value for the class labelling.
    // Get the integer for the given class value.  If there is not one, skip
    // this row and return the next row.
    String classLabel = getClassValue(resultRow, getClassQualifier());
    int classValue = attributeMap.classIndex(classLabel);
    if (classValue < 0) {
      System.out.println("No class label");
      return null;
    }

    dataPoint.set(attributeMap.classIndex(), classValue);

    // Add the feature values for the extra features.
    for (String familyAndQualifier : extraFeatureColumns) {
      String[] familyAndQualSep = familyAndQualifier.split(":");
      if (familyAndQualSep.length != 2)
        throw new IllegalArgumentException(
            "column name must be in the format of family:qualifier");

      int featureIndex = attributeMap.getExtraFeatureIndex(familyAndQualifier);

      // Ignore features that are not included in the given attribute map.
      if (featureIndex < 0)
        continue;

      double value = Double.parseDouble(new String(SchemaUtil.getColumn(
              resultRow, familyAndQualifier)));
      dataPoint.set(featureIndex, value);
    }

    boolean validFeatures = true;
    if (useDependencyPathsAsFeatures) {
      validFeatures = false;

      Map<String, Integer> pathCounts =
        WordNetEvidenceSchema.getDependencyPaths(resultRow, source);
      if (pathCounts == null || pathCounts.size() == 0) {
        System.out.println("No path counts");
        return null;
      }

      // Iterate through each of the paths for this noun pair and store the
      // number of occurances into the feature vector.
      for (Map.Entry<String, Integer> pathAndCount : pathCounts.entrySet()) {
        int value = pathAndCount.getValue();
        int featureIndex = attributeMap.getDependencyFeatureIndex(
            pathAndCount.getKey(), value);

        // Skip features that are not included in the attribute map.
        if (featureIndex < 0)
          continue;

        dataPoint.set(featureIndex, (useBinary) ? 1 : value);
        validFeatures = true;
      }
    }

    if (validFeatures)
      return dataPoint;

    System.out.println("Empty Feature Set");
    return null;
  }

  public String[] classValues() {
    return attributeMap.classValues();
  }

  public String instanceToString(DoubleVector instance) {
    int classIndex = attributeMap.classIndex();

    StringBuilder sb = new StringBuilder();
    if (instance instanceof SparseDoubleVector) {
      SparseDoubleVector sdv = (SparseDoubleVector) instance;
      int[] nz = sdv.getNonZeroIndices();
      sb.append("{");
      for (int i = 0; i < nz.length-1; ++i)
        sb.append(nz[i]).append(" ").append(sdv.get(nz[i])).append(",");
      sb.append(classIndex).append(" ");
      sb.append(attributeMap.classValues()[(int) sdv.get(classIndex)-1]);
      sb.append("}");
    } else {
      for (int i = 0; i < instance.length()-1; ++i)
        sb.append(instance.get(i)).append(",");
      sb.append(attributeMap.classValues()[(int) instance.get(classIndex)-1]);
    }
    return sb.toString();
  }

  /**
   * Sets a filter to a new {@link Scan} object such that rows with unwanted
   * column values can be ignored.
   */
  protected void setFilter(Scan scan) {
  }

  /**
   * Returns the qualifier label for the class column that determines the class
   * of each feature vector.
   */
  abstract byte[] getClassQualifier();

  /**
   * Returns the integer value of the class label in a given row for a given
   * class column qualifier, or -1 if the class label is not present for the
   * row.
   */
  abstract String getClassValue(Result resultRow, byte[] classQualifier);
}
