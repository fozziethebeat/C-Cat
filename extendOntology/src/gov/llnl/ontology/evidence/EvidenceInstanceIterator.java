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

import gov.llnl.ontology.table.WordNetEvidenceSchema;

import edu.ucla.sspace.vector.DoubleVector;

import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;

import java.io.IOException;

import java.util.Iterator;


/**
 * This iterator traverses the rows in the {@WordNetEvidenceSchema} table and
 * returns {@link Instance} values for rows that interpreted by a {@link
 * EvidenceInstanceBuilder}.  Each row can then be added to an {@link Instances}
 * object, saved to an Arff file, and used to train a Weka classifier.
 *
 * @author Keith Stevens
 */
public class EvidenceInstanceIterator implements Iterator<DoubleVector> {

  /**
   * The {@link EvidenceInstanceBuilder} that will be used to transform rows to
   * {@link Instance}s.
   */
  private EvidenceInstanceBuilder evidenceBuilder;

  /**
   * The most recent {@link Instance} that has been built.
   */
  private DoubleVector currentInstance;

  /**
   * An iterator over row results.
   */
  private Iterator<Result> resultScanner;

  /**
   * Constructs a new {@link EvidenceInstanceIterator} over the {@link
   * WordNetEvidenceSchema} table.
   */
  public EvidenceInstanceIterator(EvidenceInstanceBuilder evidenceBuilder)
      throws IOException {
    this.evidenceBuilder = evidenceBuilder;

    // Create a connection to the table.
    HTable evidenceTable = WordNetEvidenceSchema.getTable();

    // Iterate through each row.  Extract the evidence and place them into a new
    // instance for Weka training.
    Scan scan = new Scan();
    evidenceBuilder.addToScan(scan);
    ResultScanner scanner = evidenceTable.getScanner(scan);
    
    resultScanner = scanner.iterator();
    currentInstance = advance();
  }

  /**
   * {@inheritDoc}
   */
  public boolean hasNext() {
    return currentInstance != null;
  }

  /**
   * {@inheritDoc}
   */
  public DoubleVector next() {
    DoubleVector instance = currentInstance;
    currentInstance = advance();
    return instance;
  }

  /**
   * Unsupported.
   */
  public void remove() {
    throw new UnsupportedOperationException("Cannot remove values.");
  }

  /**
   * {@inheritDoc}
   */
  private DoubleVector advance() {
    if (!resultScanner.hasNext())
      return null;

    DoubleVector newInstance = evidenceBuilder.getInstanceFrom(
        resultScanner.next());
    while (newInstance == null && resultScanner.hasNext())
      newInstance = evidenceBuilder.getInstanceFrom(resultScanner.next());
    return newInstance;
  }
}
