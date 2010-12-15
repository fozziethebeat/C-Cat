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

package org.apache.mahout.classifier.sgd;

import gov.llnl.ontology.evidence.AttributeMap;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.util.SerializableUtil;

import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * An executible main for training a Logistic Regression model from data points
 * stored in text file.  Each data point is expected to be formatted to the data
 * section of the ARFF format.  The feature space, and possible class values,
 * are specified by a {@link AttributeMap}.  The trained model will be
 * serialized to disk.
 *
 * @author Keith Stevens
 */
public class TrainFromInstances {

  /**
   * The {@link AttributeMap} that specifies the number of features, number of
   * class values, and the possible class values.
   */
  private AttributeMap attributeMap;

  /**
   * The file storing training instances.
   */
  private String inputFile;

  /**
   * The file that will contain the serialized trained model.
   */
  private String outputFile;

  /**
   * A helper class for creating a {@link OnlineLogisticRegression} model from a
   * set of parameters.
   */
  private LogisticModelParameters lmp;

  /**
   * The number of training passes that should be made.
   */
  private int passes;

  /**
   * If true, then the arff file is expected to be in the sparse format.
   */
  private boolean isSparse;

  public static void main(String[] args) throws Exception {
    TrainFromInstances trainer = new TrainFromInstances();
    trainer.run(args);
  }

  /**
   * Processes the arguments, trains the model, and serializes the model to
   * disk.
   */
  public void run(String[] args) throws Exception {
    // Handle the arguments.
    if (!parseArgs(args)) {
      System.out.println("Invalid arguments");
      System.exit(1);
    }

    // Create the training model and make the requested number of passes through
    // the data set for training.
    OnlineLogisticRegression lr = lmp.createRegression();
    for (int i = 0; i < passes; ++i) {
      System.out.println("Training itration: " + i);

      // Iterate through each data point.
      BufferedReader br = new BufferedReader(new FileReader(inputFile));
      for (DataPoint d = null;
           (d = readDataPoint(br)) != null;)
        lr.train(d.value, d.dataPoint);
      br.close();
    }

    // Serialzie the trained model.
    FileWriter modelOutput = new FileWriter(outputFile);
    lmp.saveTo(modelOutput);
    modelOutput.close();
  }

  /**
   * Reads off all of the ARFF styled attribute information.
   */
  public void readHeader(BufferedReader br) throws Exception {
    for (String line = null; (line = br.readLine()) != null; ) {
      if (line.startsWith("@attribute class "))
        break;
    }

    // Read and empty line and the @data line.
    br.readLine();
    br.readLine();
  }

  /**
   * Returns a data point that contains the feature for a training instance and
   * the known class value.
   */
  public DataPoint readDataPoint(BufferedReader br) throws Exception {
    // Return null if there are no more data points.
    String line = br.readLine();
    if (line == null)
      return null;

    // Strip off the { and }.
    if (isSparse)
      line = line.trim().substring(1, line.length()-2);

    // Split the line into each feature.
    String[] features = line.split(",");

    // Get the integer value corresponding to the class label for this data
    // point.  If there is no class valid value, try to read the next data
    // point.
    String classLabel = (isSparse)
      ? features[features.length-1].split("\\s+")[1]
      : features[features.length-1];

    int classValue = attributeMap.classIndex(classLabel);
    if (classValue < 0)
      return readDataPoint(br);

    // Convert the data point to a Mahout Vector.
    Vector dataPoint = new RandomAccessSparseVector(lmp.getNumFeatures());
    for (int i = 0; i < features.length-1; ++i) {
      if (isSparse) {
        String[] feature = features[i].split("\\s+");
        dataPoint.set(
            Integer.parseInt(feature[0]), Double.parseDouble(feature[1]));
      } else {
        dataPoint.set(i, Double.parseDouble(features[i]));
      }
    }

    // Return the data point.
    return  new DataPoint(classValue, dataPoint);
  }

  /**
   * A simple struct based class for returning data points and their class
   * values.
   */
  public static class DataPoint {
    Vector dataPoint;
    int value;
    public DataPoint(int value, Vector dataPoint) {
      this.dataPoint = dataPoint;
      this.value = value;
    }
  }

  /**
   * Returns true if the arguments could be parsed and provided all of the
   * required options.  This also sets up the required class members with
   * propper values.
   */
  private boolean parseArgs(String[] args) {
    ArgOptions options = new ArgOptions();
    options.addOption('i', "input",
                      "Specifies the arff formatted file containing training " +
                      "instances",
                      true, "FILE", "Required");
    options.addOption('o', "output",
                      "Specifies the filename to which the trained " +
                      "classifier should be stored",
                      true, "FILE", "Required");
    options.addOption('a', "attributeMap",
                      "Specifies the attribute map used to create the " +
                      "training instances",
                      true, "FILE", "Required");

    options.addOption('p', "passes",
                      "Specifies the number of training passes that should " +
                      "be used.  (Default: 10)",
                      true, "INT", "Optional");
    options.addOption('s', "isSparse",
                      "Specifies that the arff training file is in the " +
                      "sparse ARFF format",
                      false, null, "optional");
    options.addOption('l', "lambda",
                      "Specifies the decay coefficient.  (Default: 1e-4",
                      true, "DOUBLE", "Optional");
    options.addOption('r', "rate",
                      "Specifies the learning rate.  (Default: 1e-3",
                      true, "DOUBLE", "Optional");
    options.addOption('b', "noBias",
                      "If set, a bias term will not be included.",
                      false, null, "Optional");
    options.parseOptions(args);

    // Validate the arguments.
    if (!options.hasOption('i') || !options.hasOption('o') ||
        !options.hasOption('a')) {
      System.out.println("Options:\n" + options.prettyPrint());
      System.exit(1);
    }

    // Read off the required options.
    inputFile = options.getStringOption('i');
    outputFile = options.getStringOption('o');
    passes = options.getIntOption('p', 10);
    isSparse = options.hasOption('s');

    // Load the attribute map.
    attributeMap = SerializableUtil.load(new File(
        options.getStringOption('a')));

    // Create the model parameters and set the number of features and number of
    // classes.
    lmp = new LogisticModelParameters();
    lmp.setMaxTargetCategories(attributeMap.classValues().length);
    lmp.setNumFeatures(attributeMap.numAttributes()-1);

    // Set the optional parameters.
    lmp.setUseBias(!options.hasOption('b'));
    lmp.setLambda(options.getDoubleOption('l', 1e-4));
    lmp.setLearningRate(options.getDoubleOption('r', 1e-3));

    return true;
  }
}
