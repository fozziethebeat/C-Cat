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

import edu.ucla.sspace.util.SerializableUtil;

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Iterator;


/**
 * An executable main class that builds a file of hypernym training instances in
 * an ARFF format.  Prior to creating the training instances, a {@link
 * AttributeMap} must be built, which defines the set of valid features for
 * the training instances, along with their indices.  
 *
 * @author Keith Stevens
 */
public class BuildHypernymInstances {

  public static void main(String[] args) 
      throws IOException, FileNotFoundException {
    if (args.length != 3) {
      throw new IllegalArgumentException("usage: java BuildHypernymInstances" +
          "source <attributeMap> <instances_name.arff>");
    }

    // Load the atribute map.
    AttributeMap attributeMap = SerializableUtil.load(new File(args[1]));

    // Create the builder and instance iterator.
    EvidenceInstanceBuilder builder = new HypernymTrainingInstanceBuilder(
        attributeMap, false, args[0]);
    Iterator<DoubleVector> instanceIter = new EvidenceInstanceIterator(builder);

    // Open up the output file and write the header information.
    PrintWriter writer = new PrintWriter(new BufferedOutputStream(
          new FileOutputStream(args[2])));
    writer.println("@relation HypernymEvidence\n");

    // Write the attribute information.  All features except for the class are
    // assumed to be numeric.
    String[] attributes = attributeMap.attributeVector();
    for (int i = 0; i < attributes.length-1; ++i)
      writer.printf("@attribute %s NUMERIC\n", attributes[i]);
    String[] classValues = builder.classValues();

    // Write the possible class values.
    writer.printf("@attribute class {%s", classValues[0]);
    for (int i = 1; i < classValues.length; ++i)
      writer.printf(",%s", classValues[i]);
    writer.println("}\n");

    writer.println("@data");

    // Write out each training instance.  If the data point is explicitly sparse
    // , store it in a sparse ARFF format, otherwise use the dense format.
    int classIndex = attributeMap.classIndex();
    while (instanceIter.hasNext())
      writer.println(builder.instanceToString(instanceIter.next()));

    writer.close();
  }
}
