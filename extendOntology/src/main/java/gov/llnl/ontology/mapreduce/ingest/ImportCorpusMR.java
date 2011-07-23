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

package gov.llnl.ontology.mapreduce.ingest;

import gov.llnl.ontology.mapreduce.table.CorpusTable;

import gov.llnl.ontology.text.DocumentReader;

import gov.llnl.ontology.text.hbase.GzipTarInputFormat;
import gov.llnl.ontology.text.hbase.XMLInputFormat;

import gov.llnl.ontology.util.MRArgOptions;

import edu.ucla.sspace.util.ReflectionUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.IdentityTableReducer;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;

import java.util.Map;


/**
 * This {@link Mapper} iterates over text documents on disk and extracts
 * various document details and the raw document text.  All of the extracted
 * information is stored in a {@link CorpusTable}.  The imported documents can
 * be extracted from two formats: a file of file paths with each path linking to
 * a gzipped tarball of documents or a list of xml files, each of which
 * contains many individual documents.
 *
 * </p>
 *
 * When processing, a {@link DocumentReader} is responsible for most of the
 * work.  The provided implementation should extract the salient meta data for
 * each document, espcially the corpus name, and the raw document text.  All of
 * this information will be saved in the specified {@link CorpusTable}.
 *
 * </p>
 *
 * This class requires that the following types of objects be specified by the
 * command line:
 * <ul>
 *   <li>{@link CorpusTable}: Controls access to the document table.</li>
 *   <li>{@link DocumentReader}: Reads meta data for each document.</li>
 * </ul>
 * @author Keith Stevens
 */
public class ImportCorpusMR extends Configured implements Tool {

    /**
     * The configuration key prefix.
     */
    public static String CONF_PREFIX =
        "gov.llnl.ontology.mapreduce.ingest.ImportCorpusMR";

    /**
     * The configuration key for setting the {@link CorpusTable}.
     */
    public static String TABLE =
        CONF_PREFIX + ".corpusTable";

    /**
     * The configuration key for setting the {@link DocumentReader}.
     */
    public static String READER=
        CONF_PREFIX + ".docReader";

    /**
     * Acquire the logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(ImportCorpusMR.class);

    /**
     * Runs the {@link IngestCorpusMR}.
     */
    public static void main(String[] args) throws Exception {
        ToolRunner.run(HBaseConfiguration.create(), new ImportCorpusMR(), args);
    }

    /**
     * {@inheritDoc}
     */
    public int run(String[] args)
            throws Exception, InterruptedException, ClassNotFoundException {
        // Setup the main arguments used.
        MRArgOptions options = new MRArgOptions();
        options.addOption('r', "docReader",
                          "Specifies the DocumentReader to use for " +
                          "splitting sentences.",
                          true, "CLASSNAME", "Required");
        options.addOption('x', "useXmlReader",
                          "Set to true if the files are in a raw xml format. " +
                          "When set, provide the tag that delimits the start " +
                          "and end of a document",
                          true, "String", "Required (One of)");
        options.addOption('g', "useGZippedReader",
                          "Set to true if the files are in a gzipped " +
                          "tarball format",
                          false, null, "Required (One of)");

        LOG.info("Parse Options");
        // Parse and validate the arguments.
        options.parseOptions(args);
        options.validate("", "<indir>+", ImportCorpusMR.class,
                         -1, 'r');

        LOG.info("Setup Configuration");
        // Setup the configuration so that the mappers know which classes to
        // use.
        Configuration conf = getConf();
        conf.set(TABLE, options.corpusTableType());
        conf.set(READER, options.getStringOption('r'));

        CorpusTable table = options.corpusTable();

        LOG.info("Setup Input Data Format");
        // Create the job and set the jar.
        Job job = new Job(conf, "Import Corpus");
        job.setJarByClass(ImportCorpusMR.class);

        job.setMapperClass(ImportCorpusMapper.class);

        // Set the input format class based on the option given.
        if (options.hasOption('g'))
            job.setInputFormatClass(GzipTarInputFormat.class);
        else if (options.hasOption('x')) {
            job.setInputFormatClass(XMLInputFormat.class);
            XMLInputFormat.setXMLTags(job, options.getStringOption('x'));
        }

        // Set the path for the input file which contains a list of files to
        // operate over
        for (int i = 0; i < options.numPositionalArgs(); ++i)
            FileInputFormat.addInputPath(
                    job, new Path(options.getPositionalArg(i)));

        LOG.info("Setup Mapper");
        // Set the table reducer.
        TableMapReduceUtil.initTableReducerJob(
                table.tableName(), IdentityTableReducer.class, job);
        job.setNumReduceTasks(0);

        LOG.info("Start ImportCorpusMapper job"); 
        // Run the job.
        job.waitForCompletion(true);
        LOG.info("Job Completed");

        return 0;
    }
        
    /**
     * This {@link Mapper} iterates over text documents on disk and extracts
     * various document details and the raw document text.  All of the extracted
     * information is stored in a {@link CorpusTable}.
     */
    public static class ImportCorpusMapper
            extends Mapper<ImmutableBytesWritable, Text, Text, Text> {
        
        /**
         * The {@link CorpusTable} responsible for saving document information.
         */
        private CorpusTable table;
        
        /**
         * The {@link DocumentReader} responsible for reading a document's data.
         */
        private DocumentReader reader;

        /**
         * {@inheritDoc}
         */
        public void setup(Context context) {
            Configuration conf = context.getConfiguration();
            table = ReflectionUtil.getObjectInstance(conf.get(TABLE));
            table.table();
            reader = ReflectionUtil.getObjectInstance(conf.get(READER));
        }

        /**
         * {@inheritDoc}
         */
        public void map(ImmutableBytesWritable key, 
                        Text value,
                        Context context) {
            table.put(reader.readDocument(value.toString()));
            context.getCounter("ImportCorpusMR", "Documents").increment(1);
        }

        /**
         * {@inheritDoc}
         */
        protected void cleanup(Context context) {
            table.close();
        }
    }
}
