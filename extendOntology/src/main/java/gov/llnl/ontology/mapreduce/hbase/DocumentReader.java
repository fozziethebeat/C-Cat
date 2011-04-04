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

package gov.llnl.ontology.hbase;

import edu.ucla.sspace.dependency.DependencyTreeNode;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;

import org.apache.hadoop.hbase.io.ImmutableBytesWritable;

import java.util.Iterator;


/**
 * An interface for interacting with a document based HBase table.  The HBase
 * table should have at least three key values for each row: the raw document
 * text, the corpus name from which the text came, and a dependency parse tree.
 * This interface allows all extraction code a fixed method for accessing these
 * data values.  Each data piece must be extractable from a {@link Result}
 * instance.  Each {@link Result} must also refer to only one document, from a
 * single source.
 *
 * </p>
 *
 * All implementations should have a no argument constructor, since the {@link
 * DocumentReader}s are often instantiated through reflection.  Implementations
 * for all methods, except for {@code setupScan} should also be stateless and
 * threadsafe.  The accessor methods will be called from multiple threads in no
 * particular order.
 *
 * @author Keith Stevens
 */
public interface DocumentReader {

  /**
   * Initializes a {@link Scan} such that it will request whatever columns and
   * column families are neccesary for extracting the raw document text,
   * dependency trees, and document source information.  This method will only
   * be called once per job.
   */
  public void setupScan(Scan scan);

  /**
   * Returns an iterator over all of the rows accessible from this {@link
   * DocumentReader}.
   */
  public Iterator<Result> tableIterator(Scan scan);

  /**
   * Returns the name of the HBase Table that this {@link DocumentReader} reads
   * from.
   */
  public String getTableName();

  /**
   * Returns the corpus name from which the document in {@code row} was
   * extracted.
   */
  public String getText(Result row);

  /**
   * Returns the document text stored in {@code row}.
   */
  public String getTextSource(Result row);

  /**
   * Returns the dependency parse tree stored in {@code row}.
   */
  public DependencyTreeNode[] getDependencyTree(Result row);

  /**
   * Returns true if the given {@code row} should be processed.
   */
  public boolean shouldProcessRow(Result row);

  /**
   * Marks the row index by {@code key} as having been processed.
   */
  public void markRowAsProcessed(ImmutableBytesWritable key, Result row);

  /**
   * Closes the connection to the document reader.
   */
  public void close();
}
