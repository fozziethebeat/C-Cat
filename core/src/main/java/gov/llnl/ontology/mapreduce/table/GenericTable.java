/*
 * Copyright (c) 2011, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
 *
 * This file is part of the C-Cat package and is covered under the terms and
 * conditions therein.
 *
 * The C-Cat package is free software: you can redistribute it and/or modify
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

import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;

import java.util.Iterator;


/**
 * @author Keith Stevens
 */
public interface GenericTable {

    /**
     * Creates a new instance of the {@link HTable} represented by this {@link
     * GenericTable}
     */
    void createTable();

    /**
     * Creates a new instance of the {@link HTable} represented by this {@link
     * GenericTable}
     */
    void createTable(HConnection connector);

    /**
     * Returns an iterator over all of the rows accessible from this {@link
     * GenericTable}.
     */
    Iterator<Result> iterator(Scan scan);

    /**
     * Returns the name of the HBase Table that this {@link GenericTable}
     * represents.
     */
    String tableName();

    /**
     * Returns the {@link HTable} instance attached to this {@link
     * GenericTable}.
     */
    HTable table();

    /**
     * Initializes a {@link Scan} such that it will request whatever columns and
     * column families are neccesary for processing as determined by the table
     * type.  This method will only be called once per job.
     */
    void setupScan(Scan scan);

    /**
     * Initializes a {@link Scan} such that it will request columns and
     * column families are neccesary for extracting the raw document text,
     * dependency trees, and document source information from the specified
     * {@code corpusName}. 
     */
    void setupScan(Scan scan, String corpusName);

    /**
     * Closes the connection to the document reader.
     */
    void close();
}
