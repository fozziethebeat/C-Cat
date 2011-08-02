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

import gov.llnl.ontology.text.Document;
import gov.llnl.ontology.text.Sentence;

import com.google.gson.reflect.TypeToken;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.ZooKeeperConnectionException;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HConnectionManager;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;

import java.io.IOError;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * @author Keith Stevens
 */
public class TrinidadTable implements CorpusTable {

    /**
     * Stores the text type of any document.
     */
    public static final String XML_MIME_TYPE = "text/xml";

    /**
     * A marker to request all corpora types when scanning.
     */
    public static final String ALL_CORPORA = "";

    /**
     * The official table name.
     */
    public static final String TABLE_NAME = "trinidad_table";

    /**
     * The column family for source related columns.
     */
    public static final String SOURCE_CF = "src";

    /**
     * The column qualifier for the corpus source name.
     */
    public static final String SOURCE_NAME = "name";

    /**
     * The full column qualifier for the corpus source name.
     */
    public static final String SOURCE_NAMECOL = SOURCE_CF + ":" + SOURCE_NAME;

    /**
     * The column qualifier for the corpus id.
     */
    public static final String SOURCE_ID = "id";

    /**
     * The full column qualifier for the corpus id.
     */
    public static final String SOURCE_IDCOL = SOURCE_CF + ":" + SOURCE_ID;

    /**
     * The column family for the text colunns.
     */
    public static final String TEXT_CF = "text";

    /**
     * The column qualifier for the original document text.
     */
    public static final String TEXT_ORIGINAL = "orig";

    /**
     * The full column qualifier for the original document text.
     */
    public static final String TEXT_ORIGINAL_COL =
        TEXT_CF + ":" + TEXT_ORIGINAL;

    /**
     * The column qualifier for the text type.
     */
    public static final String TEXT_TYPE = "text";

    /**
     * The full column qualifier for the text type.
     */
    public static final String TEXT_TYPE_COL = TEXT_CF + ":" + TEXT_TYPE;

    /**
     * The column qualifier for the cleaned document text.
     */
    public static final String TEXT_RAW = "raw";
    
    /**
     * The full column qualifier for the cleaned document text.
     */
    public static final String TEXT_RAW_COL = TEXT_CF + ":" + TEXT_RAW;

    /**
     * The column qualifier for the document title.
     */
    public static final String TEXT_TITLE = "title";

    /**
     * The full column qualifier for the document title.
     */
    public static final String TEXT_TITLE_COL = TEXT_CF + ":" + TEXT_TITLE;

    /**
     * The column family for the document annotations.
     */
    public static final String ANNOTATION_CF = "annotations";
    
    /**
     * The column qualifier for the sentence level document annotations.
     */
    public static final String ANNOTATION_SENTENCE = "sentence";

    /**
     * The column family for word list labels associated wtih each document.
     */
    public static final String LABEL_CF = "wordListLabels";

    /**
     * The column family for word list labels associated wtih each document.
     */
    public static final String META_CF = "meta";

    /**
     * The column name for categories that a document may fall under, if any.
     */
    public static final String CATEGORY_COLUMN = "categories";

    /**
     * A connection to the {@link HTable}.
     */
    private HTable table;

    /**
     * Creates a new {@link TrinidadTable} that uses the default {@lin
     * HBaseConfiguration}.
     */
    public TrinidadTable() {
        table = null;
    }

    /**
     * {@inheritDoc}
     */
    public void createTable() {
        try {
            Configuration conf = HBaseConfiguration.create();
            HConnection connector = HConnectionManager.getConnection(conf);
            createTable(connector);
        } catch (org.apache.hadoop.hbase.ZooKeeperConnectionException zkce) {
            zkce.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void createTable(HConnection connector) {
        try {
            Configuration config = HBaseConfiguration.create();
            HBaseAdmin admin = new HBaseAdmin(config);

            // Do nothing if the table already exists.
            if (admin.tableExists(TABLE_NAME)) 
                return;

            HTableDescriptor docDesc = new HTableDescriptor(TABLE_NAME);
            SchemaUtil.addDefaultColumnFamily(docDesc, SOURCE_CF);
            SchemaUtil.addDefaultColumnFamily(docDesc, TEXT_CF);
            SchemaUtil.addDefaultColumnFamily(docDesc, ANNOTATION_CF);
            SchemaUtil.addDefaultColumnFamily(docDesc, LABEL_CF);
            SchemaUtil.addDefaultColumnFamily(docDesc, META_CF);
            admin.createTable(docDesc);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setupScan(Scan scan) {
        setupScan(scan, ALL_CORPORA);
    }

    /**
     * {@inheritDoc}
     */
    public void setupScan(Scan scan, String corpusName) {
        scan.addColumn(SOURCE_CF.getBytes(), SOURCE_NAME.getBytes());
        scan.addColumn(ANNOTATION_CF.getBytes(),
                       ANNOTATION_SENTENCE.getBytes());
        scan.addFamily(TEXT_CF.getBytes());
        scan.addFamily(LABEL_CF.getBytes());
        scan.addFamily(META_CF.getBytes());
        if (!corpusName.equals(ALL_CORPORA))
            scan.setFilter(new SingleColumnValueFilter(SOURCE_CF.getBytes(),
                                                       SOURCE_NAME.getBytes(), 
                                                       CompareOp.EQUAL,
                                                       corpusName.getBytes()));
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Result> iterator(Scan scan) {
        try {
            return table.getScanner(scan).iterator();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String tableName() {
        return TABLE_NAME;
    }

    /**
     * {@inheritDoc}
     */
    public HTable table() {
        try {
            table = new HTable(HBaseConfiguration.create(), TABLE_NAME);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
        return table;
    }

    /**
     * {@inheritDoc}
     */
    public String text(Result row) {
        return SchemaUtil.getColumn(row, TEXT_CF, TEXT_RAW);
    }

    /**
     * {@inheritDoc}
     */
    public String textSource(Result row) {
        return SchemaUtil.getColumn(row, TEXT_CF, TEXT_ORIGINAL);
    }

    /**
     * {@inheritDoc}
     */
    public String sourceCorpus(Result row) {
        return SchemaUtil.getColumn(row, SOURCE_CF, SOURCE_NAME);
    }

    /**
     * {@inheritDoc}
     */
    public String title(Result row) {
        return SchemaUtil.getColumn(row, TEXT_CF, TEXT_TITLE);
    }

    /**
     * {@inheritDoc}
     */
    public List<Sentence> sentences(Result row) {
        return SchemaUtil.getObjectColumn(
                row, ANNOTATION_CF, ANNOTATION_SENTENCE,
                new TypeToken<List<Sentence>>(){}.getType());
    }

    /**
     * {@inheritDoc}
     */
    public void put(Document document) {
        Put put = new Put(DigestUtils.shaHex(document.key()).getBytes());
        SchemaUtil.add(put, SOURCE_CF, SOURCE_NAME, document.sourceCorpus());
        SchemaUtil.add(put, TEXT_CF, TEXT_ORIGINAL, document.originalText());
        SchemaUtil.add(put, TEXT_CF, TEXT_RAW, document.rawText());
        SchemaUtil.add(put, TEXT_CF, TEXT_TITLE, document.title());
        SchemaUtil.add(put, TEXT_CF, TEXT_TYPE, XML_MIME_TYPE);
        SchemaUtil.add(put, META_CF, CATEGORY_COLUMN, document.categories());
        put(put);
    }

    /**
     * {@inheritDoc}
     */
    public void put(ImmutableBytesWritable key, List<Sentence> sentences) {
        Put put = new Put(key.get());
        SchemaUtil.add(put, ANNOTATION_CF, ANNOTATION_SENTENCE, sentences);
        put(put);
    }

    /**
     * Stores the {@code put} into the {@link HTable}.  This helper method just
     * encapsulates the error handling code.
     */
    private void put(Put put) {
        try {
            table.put(put);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void putCategories(ImmutableBytesWritable key,
                              Set<String> categories) {
        Put put = new Put(key.get());
        SchemaUtil.add(put, META_CF, CATEGORY_COLUMN, categories);
        put(put);
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getCategories(Result row) {
        return SchemaUtil.getObjectColumn(
                row, META_CF, CATEGORY_COLUMN,
                new TypeToken<Set<String>>(){}.getType());
    }

    /**
     * {@inheritDoc}
     */
    public void putLabel(ImmutableBytesWritable key,
                         String labelName,
                         String labelValue) {
        Put put = new Put(key.get());
        SchemaUtil.add(put, LABEL_CF, labelName, labelValue);
        put(put);
    }

    /**
     * {@inheritDoc}
     */
    public String getLabel(Result row, String labelName) {
        return SchemaUtil.getColumn(row, LABEL_CF, labelName);
    }

    /**
     * {@inheritDoc}
     */
    public boolean shouldProcessRow(Result row) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void markRowAsProcessed(ImmutableBytesWritable key, Result row) {
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        try {
            table.flushCommits();
            table.close();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }
}
