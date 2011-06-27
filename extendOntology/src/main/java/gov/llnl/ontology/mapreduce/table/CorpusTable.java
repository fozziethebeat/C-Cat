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

package gov.llnl.ontology.mapreduce.table;

import gov.llnl.ontology.text.Document;
import gov.llnl.ontology.text.Sentence;

import edu.ucla.sspace.dependency.DependencyTreeNode;

import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;

import org.apache.hadoop.hbase.io.ImmutableBytesWritable;

import java.util.Iterator;
import java.util.List;


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
public interface CorpusTable extends Iterator<Result> {

    /**
     * Initializes a {@link Scan} such that it will request whatever columns and
     * column families are neccesary for extracting the raw document text,
     * dependency trees, and document source information.    This method will
     * only be called once per job.
     */
    void setupScan(Scan scan);

    /**
     * Initializes a {@link Scan} such that it will request columns and
     * column families are neccesary for extracting the raw document text,
     * dependency trees, and document source information from the specified
     * {@code corpusName}.  Dependency parse columns will only be scanned if
     * {@code getDepParse} is true.  This method will only be called once per
     * job.
     */
    void setupScan(Scan scan, String corpusName, boolean getDepParse);

    /**
     * Returns an iterator over all of the rows accessible from this {@link
     * DocumentReader}.
     */
    Iterator<Result> iterator(Scan scan);

    /**
     * Returns the name of the HBase Table that this {@link DocumentReader}
     * reads from.
     */
    String tableName();

    /**
     * Returns the {@lin HTable} instance attached to this {@link CorpusTable}.
     */
    HTable table();

    /**
     * Returns the cleaned text stored by the given {@code row}.
     */
    String text(Result row);

    /**
     * Returns the document text stored in {@code row}.
     */
    String textSource(Result row);

    /**
     * Returns the {@link List} of {@link Sentence}s stored in {@code row}.
     * This call will include all annotations requested in the setup call to
     * {@link #setupScan}.
     */
    List<Sentence> sentences(Result row);

    /**
     * Stores the text of {@link Document} in this {@link CorpusTable}.
     */
    void put(Document document);

    /**
     * Stores the {@link List} of {@link Sentences} in this table.
     * Implementations are welcome to stores this {@link List} as a complete
     * object or as a seperate set of smaller {@link Annotation}s.
     */
    void put(ImmutableBytesWritable key, List<Sentence> sentences);

    /**
     * Returns true if the given {@code row} should be processed.
     */
    boolean shouldProcessRow(Result row);

    /**
     * Marks the row index by {@code key} as having been processed.
     */
    void markRowAsProcessed(ImmutableBytesWritable key, Result row);

    /**
     * Closes the connection to the document reader.
     */
    void close();
}
