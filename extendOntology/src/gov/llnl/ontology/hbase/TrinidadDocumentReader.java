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

import gov.llnl.ontology.table.SchemaUtil;

import edu.ucla.sspace.dependency.DependencyRelation;
import edu.ucla.sspace.dependency.DependencyTreeNode;
import edu.ucla.sspace.dependency.SimpleDependencyRelation;
import edu.ucla.sspace.dependency.SimpleDependencyTreeNode;

import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;

import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;

import org.apache.hadoop.hbase.io.ImmutableBytesWritable;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;

import trinidad.hbase.table.DocSchema;

import java.io.IOError;
import java.io.IOException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * A {@link DocumentReader} for Trinidad HBase documents.
 *
 * @see DocSchema
 * @author Keith Stevens
 */
public class TrinidadDocumentReader implements DocumentReader {

  /**
   * The base property prefix.
   */
  public static final String PROPERTY_PREFIX =
    "gov.llnl.ontology.hbase.TrinidadDocumentReader";

  /**
   * The property for setting the document source.  By setting this, the reader
   * will only return documents from this particular source.
   */
  public static final String SOURCE_PROPERTY =
    PROPERTY_PREFIX + ".source";

  /**
   * The {@link HTable} holding the trinidad processed documents.
   */
  private final HTable docTable;

  /**
   * Creates a new {@link TrinidadDocumentReader}.
   */
  public TrinidadDocumentReader() {
    try {
      docTable = DocSchema.getTable();
    } catch (IOException ioe) {
      throw new IOError(ioe);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void setupScan(Scan scan) {
    scan.addFamily(DocSchema.textCF.getBytes());
    scan.addColumn(DocSchema.annotationsCF.getBytes(),
                   DocSchema.annotationsDep.getBytes());
    scan.addColumn(DocSchema.annotationsCF.getBytes(),
                   DocSchema.annotationsPOS.getBytes());
    scan.addColumn(DocSchema.annotationsCF.getBytes(),
                   DocSchema.annotationsToken.getBytes());
    scan.addColumn(DocSchema.annotationsCF.getBytes(),
                   DocSchema.annotationsSentence.getBytes());
    scan.addColumn(DocSchema.srcCF.getBytes(), DocSchema.srcName.getBytes());

    // Limite the documents traversed to the requested source.
    String source = System.getProperties().getProperty(SOURCE_PROPERTY);
    if (source != null)
      scan.setFilter(
          new SingleColumnValueFilter(
            DocSchema.srcCF.getBytes(), DocSchema.srcName.getBytes(),
            CompareOp.EQUAL, source.getBytes()));
  }

  /**
   * {@inheritDoc}
   */
  public Iterator<Result> tableIterator(Scan scan) {
    try {
      ResultScanner scanner = docTable.getScanner(scan);
      return scanner.iterator();
    } catch (IOException ioe) {
      throw new IOError(ioe);
    }
  }

  /**
   * {@inheritDoc}
   */
  public String getTableName() {
    return DocSchema.tableName;
  }

  /**
   * {@inheritDoc}
   */
  public String getText(Result row) {
      return DocSchema.getColumn(row, DocSchema.textCF, DocSchema.textRaw);
  }

  /**
   * {@inheritDoc}
   */
  public String getTextSource(Result row) {
      return DocSchema.getColumn(row, DocSchema.srcCF, DocSchema.srcName);
  }

  /**
   * {@inheritDoc}
   */
  public DependencyTreeNode[] getDependencyTree(Result row) {
    // Get the document text and annotations for dependency parses and parts of
    // speech tags.
    String docText = DocSchema.getRawText(row);
    AnnotationSet dependencyAnnots = DocSchema.getAnnotationSet(
        row, DocSchema.annotationsDep);
    AnnotationSet posAnnots = DocSchema.getAnnotationSet(
        row, DocSchema.annotationsPOS);

    return extractFromAnnotations(docText, dependencyAnnots, posAnnots);
  }

  /**
   * Creates a dependency tree from {@link AnnotationSet}s in the Trinidad HBase
   * table.  Each token is cleaned of junk characters prior to being turned into
   * a the dependency tree.
   */
  public static DependencyTreeNode[] extractFromAnnotations(
      String docText,
      AnnotationSet dependencyAnnots,
      AnnotationSet posAnnots) {
    // Return null for rows that lack part of speech tags and dependency trees.
    if (dependencyAnnots == null || posAnnots == null)
      return null;

    // For each pos annotation, create a dependency tree node with the
    // annotations term and part of speech information.  Also create a mapping
    // from the offset values to the node itself so that when passing through
    // the dependency annotations, we can find the governing nodes easily.
    Map<String, SimpleDependencyTreeNode> governingMap =
      new HashMap<String, SimpleDependencyTreeNode>();

    int numNodes = posAnnots.size();
    SimpleDependencyTreeNode[] parseTree =
      new SimpleDependencyTreeNode[numNodes]; 
    int treeIndex = 0;
    for (Annotation posAnnot : posAnnots) {
      String term = docText.substring(
          posAnnot.getStartOffset(), posAnnot.getEndOffset());

      // Remove a handful of extra characters that should not be part of the
      // tokenized word and trim any generated white space on either side of the
      // term.
      while (term.length() > 0 && term.startsWith("-"))
        term = term.substring(1, term.length());
      while (term.length() > 0 && term.endsWith("-"))
        term = term.substring(0, term.length()-1);
      term = term.replaceAll("\"", "");
      term = term.replaceAll("\'", "");
      term = term.replaceAll("\\[", "");
      term = term.replaceAll("\\]", "");
      term = term.replaceAll("\\?", "");
      term = term.replaceAll("\\*", "");
      term = term.replaceAll("\\(", "");
      term = term.replaceAll("\\)", "");
      term = term.replaceAll("\\^", "");
      term = term.replaceAll("\\+", "");
      term = term.replaceAll("//", "");
      term = term.replaceAll(";", "");
      term = term.replaceAll("%", "");
      term = term.replaceAll(",", "");
      term = term.replaceAll("!", "");
      term = term.trim();

      // Save a tree node for the term and pos.  Give it an id based on the
      // start and ending offset so that references from the dependency
      // annotations can easily retrieve the needed tree nodes.
      String pos = posAnnot.getType();
      parseTree[treeIndex] = new SimpleDependencyTreeNode(term, pos);
      String nodeId = posAnnot.getStartOffset() + "," + posAnnot.getEndOffset();
      governingMap.put(nodeId, parseTree[treeIndex]);
      treeIndex++;
    }

    // Iterate through each of the dependency annotation.  For each one, get the
    // tree node for the child and the parent.  Extract the relation between the
    // two nodes and add a relation link between the nodes.
    for (Annotation depAnnot : dependencyAnnots) {
      // Extract the ids for each node and the relation.
      String nodeId = depAnnot.getStartOffset() + "," + depAnnot.getEndOffset();
      String governId = depAnnot.getAttribute("GOV");

      // Why is there a null governId? Does this correspond to a root in the
      // tree?
      if (governId == null)
        continue;
      String relationType = depAnnot.getType();

      // Get the two relevant nodes.
      SimpleDependencyTreeNode childNode = governingMap.get(nodeId);
      SimpleDependencyTreeNode parentNode = governingMap.get(governId);

      // Add the link between the two nodes.
      DependencyRelation relation = new SimpleDependencyRelation(
          parentNode, relationType, childNode);
      childNode.addNeighbor(relation);
      parentNode.addNeighbor(relation);
    }

    return parseTree;
  }

  /**
   * {@inheritDoc}
   */
  public boolean shouldProcessRow(Result row) {
    String processed = DocSchema.getColumn(
        row, DocSchema.annotationsCF, "evidenceExtract");
    return processed == null;
  }

  /**
   * {@inheritDoc}
   */
  public void markRowAsProcessed(ImmutableBytesWritable key, Result row) {
    try {
      Put put = new Put(key.get());
      DocSchema.add(put, DocSchema.annotationsCF,
                    "evidenceExtract", "processed");
      docTable.put(put);
    } catch (IOException ioe) {
      throw new IOError(ioe);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void close() {
    try {
      docTable.flushCommits();
      docTable.close();
    } catch (IOException ioe) {
      throw new IOError(ioe);
    }
  }
}
