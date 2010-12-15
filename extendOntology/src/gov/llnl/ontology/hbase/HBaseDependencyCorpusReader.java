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

import edu.ucla.sspace.dependency.DependencyRelation;
import edu.ucla.sspace.dependency.DependencyTreeNode;

import edu.ucla.sspace.text.Document;
import edu.ucla.sspace.text.StringDocument;
import edu.ucla.sspace.text.DocumentPreprocessor;

import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;

import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;

import java.io.IOError;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Properties;


/**
 * This corpus reader iterates over an hbase table based on the {@link
 * DocSchema}.  If a corpus source is specified, this reader will only cover
 * documents from the specified source, otherwise all documents will be covered.
 */
public class HBaseDependencyCorpusReader implements Iterator<Document> {
  
  public static final String SEPARATOR = "|||||";

  /**
   * The text of the next document to return.
   */
  private String nextLine;

  /**
   * An {@link Iterator} of rows in the HBase table.
   */
  private Iterator<Result> resultScanner;

  /**
   * The {@link DocumentReader} used to extract dependency parse trees from the
   * hbase table.
   */
  private DocumentReader reader;

  /**
   * Creates a new {@link HBaseDependencyCorpusReader} that uses the system
   * provided properties.
   */
  public HBaseDependencyCorpusReader() {
    this(System.getProperties());
  }

  /**
   * Creates a new {@link HBaseDependencyCorpusReader} that uses provided
   * properties.
   */
  public HBaseDependencyCorpusReader(Properties props) {
    // Create a new scan that covers the text column family and the source
    // column family.
    reader = new TrinidadDocumentReader();
    Scan scan = new Scan();
    reader.setupScan(scan);

    // Get the row iterator and acquire the first document to return.
    resultScanner = reader.tableIterator(scan);
    nextLine = advance();
  }

  /**
   * {@inheritDoc}
   */
  public boolean hasNext() {
    return nextLine != null;
  }
  
  /**
   * {@inheritDoc}
   */
  public Document next() {
    Document doc = new StringDocument(nextLine);
    nextLine = advance();
    return doc;
  }

  /**
   * {@inheritDoc}
   */
  public void remove() {
    throw new UnsupportedOperationException("Cannot remove values.");
  }

  /**
   * Returns the text or the next document to be returned or {@code null} if
   * there are no more valid documents.
   */
  private String advance() {
    if (!resultScanner.hasNext())
      return null;

    DependencyTreeNode[] tree = null;
    while (resultScanner.hasNext() &&
           (tree = reader.getDependencyTree(resultScanner.next())) == null)
      ;
    if (tree == null)
      return null;

    Map<DependencyTreeNode, Integer> nodeIndex =
      new HashMap<DependencyTreeNode, Integer>();
    for (int i = 0; i < tree.length; ++i)
      nodeIndex.put(tree[i], i);

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < tree.length; ++i) {
      sb.append(i+1).append("\t");
      sb.append(tree[i].word()).append("\t_\t");
      sb.append(tree[i].pos()).append("\t");
      sb.append(tree[i].pos()).append("\t_\t");

      for (DependencyRelation link : tree[i].neighbors()) {
        if (link.dependentNode() == tree[i]) {
          sb.append(nodeIndex.get(link.headNode())).append("\t");
          sb.append(link.relation()).append("\n");
          break;
        }
      }
    }

    return sb.toString();
  }
}

