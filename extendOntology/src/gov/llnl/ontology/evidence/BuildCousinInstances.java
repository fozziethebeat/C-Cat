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

import edu.ucla.sspace.util.SerializableUtil;

import weka.core.Instance;
import weka.core.Instances;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Iterator;


/**
 * @author Keith Stevens
public class BuildCousinInstances {

  public static void main(String[] args) 
      throws IOException, FileNotFoundException {
    if (args.length != 2) {
      throw new IllegalArgumentException("usage: java BuildCousinInstances" +
          "attributeMap.bin instances_name.arff");
    }

    WekaAttributeMap attributeMap = SerializableUtil.load(new File(args[0]));

    EvidenceInstanceBuilder builder = new CousinTrainingInstanceBuilder(
        attributeMap, false, 7);
    Iterator<Instance> instanceIter = new EvidenceInstanceIterator(builder); 

    Instances cousinInstances = new Instances("CousinEvidence",
        attributeMap.attributeVector(), 1000);
    while (instanceIter.hasNext())
      cousinInstances.add(instanceIter.next());

    PrintWriter writer = new PrintWriter(new BufferedOutputStream(
          new FileOutputStream(args[1])));
    writer.write(cousinInstances.toString());
    writer.close();
  }
}
 */
