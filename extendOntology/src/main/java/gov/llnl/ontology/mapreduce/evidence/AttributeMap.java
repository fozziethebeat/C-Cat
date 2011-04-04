/*
 * Copyright (c) 2008, Lawrence Livermore National Security, LLC. Produced at
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

import gov.llnl.ontology.table.WordNetEvidenceSchema;

import gov.llnl.ontology.wordnet.SynsetRelations.HypernymStatus;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.util.SerializableUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.io.Serializable;

import java.util.Map;
import java.util.HashMap;


/**
 * This class builds an attribute map to be used for building training and
 * testing {@link Instance}s for .  The last index is reserved for the
 * class label.  Attributes 0 through |num_unique_extra_features|-1 will be for
 * extra dependency features, and the rest will be dependency path features.
 *
 * @author Keith Stevens
 */
public class AttributeMap implements Serializable {

  /**
   * A mapping from feature labels to their indices in a sparse vector.  The
   * index also corresponds to a {@link Attribute} which  uses as meta data
   * for the feature.
   */
  protected Map<String, Integer> extraFeatureLabelToIndex;

  /**
   * A mapping from dependency feature labels to their indicies in a sparse
   * vector.  The index also corresponds to a {@link Attribute} in the attribute
   * vector.
   */
  protected Map<String, Integer> dependencyFeatureLabelToIndex;

  /**
   * The vector of attributes to be used by a {@link EvidenceInstanceBuilder}
   * and any {@link Instances} built from the builder.
   */
  protected String[] attributeVector;
   
  /**
   * The set of class labels used with this {@link AttributeMap}.
   */
  protected String[] classValues;

  /**
   * A mapping from class labels to their integer values.
   */
  protected Map<String, Integer> classMap;

  /**
   * Creates a new {@link BuildInstance}.
   *
   * @param extraFeatureColumns The string of column names that should be used
   *        as additional features
   * @param observedDependencyPathsFile A file containing all of the observed
   *        dependency paths and their frequency
   * @param pathCountThreshold The minimum number of times a dependency path
   *        must occur in order for it to be considered a valid feature
   */
  public AttributeMap(String[] extraFeatureColumns,
                          Map<String, Integer> dependencyPathCounts,
                          int pathCountThreshold)
      throws IOException {
    classMap = new HashMap<String, Integer>();

    // Setup common objects.
    extraFeatureLabelToIndex = new HashMap<String, Integer>();
    dependencyFeatureLabelToIndex = new HashMap<String, Integer>();

    for (Map.Entry<String, Integer> entry : dependencyPathCounts.entrySet())
      if (entry.getValue() > pathCountThreshold)
        dependencyFeatureLabelToIndex.put(
            entry.getKey(), dependencyFeatureLabelToIndex.size());

    expandAttributes(dependencyFeatureLabelToIndex);

    int featureOffset = extraFeatureIndexStart();
    for (String extraFeature : extraFeatureColumns) {
      if (extraFeature.length() == 0)
        continue;
      extraFeatureLabelToIndex.put(extraFeature, featureOffset - 1);
      featureOffset++;
    }

    // Create the fast vector needed to specify the attribues.
    attributeVector = new String[numAttributes()];

    // Add the extra feature attributes.
    featureOffset = extraFeatureIndexStart();
    for (Map.Entry<String, Integer> entry : extraFeatureLabelToIndex.entrySet())
      attributeVector[entry.getValue() + featureOffset] = entry.getKey();

    // Add the dependency feature attributes.
    featureOffset = dependencyFeatureIndexStart();
    for (Map.Entry<String, Integer> entry : 
        dependencyFeatureLabelToIndex.entrySet())
      attributeVector[entry.getValue() + featureOffset] = entry.getKey();

    attributeVector[classIndex()] = "class";
  }

  /**
   * Returns a mapping from dependency paths to their frequency counts.
   */
  public static Map<String, Integer> getDependencyCounts(
      String observedDependencyPathsFile) {
    Map<String, Integer> pathCounts = new HashMap<String, Integer>();
    try {
      if (observedDependencyPathsFile != null) {
        BufferedReader br = new BufferedReader(new FileReader(
              observedDependencyPathsFile));
        for (String line = null; (line = br.readLine()) != null; ) {
          String[] tokens = line.split("\\s+");
          pathCounts.put(tokens[0], Integer.parseInt(tokens[1]));
        }
      }
    } catch (IOException ioe) {
      throw new IOError(ioe);
    }
    return pathCounts;
  }

  /**
   * Returns the array of class labels used.
   */
  public void setClassValues(boolean useHypernymLabels) {
    if (useHypernymLabels) {
      classValues = new String[2];
      classValues[0] = HypernymStatus.KNOWN_HYPERNYM.toString();
      classValues[1] = HypernymStatus.KNOWN_NON_HYPERNYM.toString();
    } else {
      classValues = new String[(int)Math.pow(7+1, 2) + 1];

      int count = 0;

      classValues[count++] = Integer.MAX_VALUE + "-" + Integer.MAX_VALUE;

      for (int i = 0; i < 7; ++i) {
        for (int j = 0; j <= i; ++j) 
          classValues[count++] = i + "-" + j;
        classValues[count++] = Integer.MAX_VALUE + "-" + i;
      }
    }

    for (int i = 0; i < classValues.length; ++i) {
      classMap.put(classValues[i], i);
      System.out.println(classValues[i]);
    }
  }

  /**
   * Returns the array of class labels used.
   */
  public String[] classValues() {
    return classValues;
  }

  public int classIndex(String classLabel) {
    Integer value = classMap.get(classLabel);
    return (value == null) ? -1 : value;
  }

  /**
   * Returns an array of Strings that provide descriptions of each feature.
   */
  public String[] attributeVector() {
    return attributeVector;
  }

  /**
   * The index at which class values should be stored in feature vectors.
   */
  public int classIndex() {
    return attributeVector.length - 1;
  }

  /**
   * The first index at which extra feature values should be stored in feature
   * vectors.
   */
  public int extraFeatureIndexStart() {
    return 0;
  }

  /**
   * The first index at which dependency path feature values should be stored in
   * feature vectors.
   */
  public int dependencyFeatureIndexStart() {
    return extraFeatureLabelToIndex.size();
  }

  /**
   * Returns the index for a given extra feature, based on the feature label,
   * .e.g. the feature's column in the schema.
   */
  public int getExtraFeatureIndex(String featureLabel) {
    Integer index = extraFeatureLabelToIndex.get(featureLabel);
    if (index == null)
      return -1;
    return extraFeatureIndexStart() + index.intValue();
  }
 
  /**
   * Returns the index for a given dependency feature, based on dependency path
   * and the number of times the path has occured.
   */
  public int getDependencyFeatureIndex(String featureLabel, double count) {
    Integer index = dependencyFeatureLabelToIndex.get(featureLabel);
    if (index == null)
      return -1;
    return dependencyFeatureIndexStart() + index.intValue();
  }

  /**
   * Returns the total number of attributes found by this map so far.
   */
  public int numAttributes() {
    return 1 + dependencyFeatureLabelToIndex.size() +
               extraFeatureLabelToIndex.size();
  }

  /**
   * Returns the column names for extra features.
   */
  public String[] extraFeatureColumns() {
    return extraFeatureLabelToIndex.entrySet().toArray(new String[0]);
  }

  /**
   * Returns true if there are valid dependency paths that can be used as
   * features.
   */
  public boolean useDependencyPathsAsFeatures() {
    return dependencyFeatureLabelToIndex.size() > 0;
  }

  /**
   * Expands the the given feature map based on some deterministic process.  In
   * this base class, this is a noop.
   */
  protected void expandAttributes(Map<String, Integer> featureMap) {
  }

  public static void main(String[] args) throws IOException {
    ArgOptions options = new ArgOptions();
    options.addOption('l', "labelType",
                      "Specifies the type of class labels that will be used",
                      true, "cousin|hypernym", "Required");
    options.addOption('e', "useExtendedAttributeMap",
                      "Set if the ExtendedAttributeMap should be used",
                      false, null, "Optional");
    options.addOption('b', "numBuckets",
                      "Set the number of replicated buckets to use with the " +
                      "ExtendedAttributeMap (Default : 14)",
                      true, "INT", "Optional");
    options.addOption('f', "extraFeatures",
                      "A comma separated list of columns to be used as " +
                      "features",
                      true, "COLUMN[,COLUMN]+", "Required (At least one of)");
    options.addOption('d', "dependencyPathCounts",
                      "A file containing observed dependency paths",
                      true, "FILE", "Required (At least one of)");
    options.addOption('t', "dependencyPathThreshold",
                      "The minimum number of times a path should occur to " +
                      "count as a feature (Default: 1)",
                      true, "INT", "Optional");
    options.parseOptions(args);

    if (options.numPositionalArgs() == 0 ||
        (!options.hasOption('d') && !options.hasOption('f') && 
         !options.hasOption('l'))) {
      System.out.println(
          "usage: java AttributeMap [OPTIONS] atributeMap.bin\n" +
          options.prettyPrint());
      System.exit(1);
    }

    String[] extraFeatures = options.getStringOption('f', "").split(",");
    String pathCountFile = options.getStringOption('d');
    int threshold = options.getIntOption('t', 2);
    int numBuckets = options.getIntOption('b', 14);

    boolean useExtended = options.hasOption('e');

    Map<String, Integer> pathCounts = getDependencyCounts(pathCountFile);
    AttributeMap attributeMap = (useExtended)
      ? new ExtendedAttributeMap(
          extraFeatures, pathCounts, threshold, numBuckets)
      : new AttributeMap(extraFeatures, pathCounts, threshold);

    boolean useHypernymLabels = options.getStringOption('l').equals("hypernym");
    attributeMap.setClassValues(useHypernymLabels);

    SerializableUtil.save(attributeMap, new File(options.getPositionalArg(0)));
  }
}
