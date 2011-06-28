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


/**
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
     * Runs the {@link IngestCorpusMR}.
     */
    public static void main(String[] args) throws Exception {
        ToolRunner.run(new Configuration(), new ImportCorpusMR(), args);
    }

    /**
     * {@inheritDoc}
     */
    public int run(String[] args)
            throws IOException, InterruptedException, ClassNotFoundException {
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

        // Parse and validate the arguments.
        options.parseOptions(args);
        if (!options.hasOption('r')) {
            System.err.println("usage: java ImportCorpusMR [OPTIONS] <indir>\n"+
                               options.prettyPrint());
            System.exit(1);
        }

        // Setup the configuration so that the mappers know which classes to
        // use.
        Configuration conf = new HBaseConfiguration();
        conf.set(TABLE, options.corpusTableType());
        conf.set(READER, options.getStringOption('r'));

        CorpusTable table = options.corpusTable();

        // Create the job and set the jar.
        Job job = new Job(conf, "Import Corpus");
        job.setJarByClass(ImportCorpusMR.class);

        job.setMapperClass(ImportCorpusMapper.class);

        if (options.hasOption('g'))
            job.setInputFormatClass(GzipTarInputFormat.class);
        else if (options.hasOption('x')) {
            job.setInputFormatClass(XMLInputFormat.class);
            XMLInputFormat.setXMLTags(job, options.getStringOption('x'));
        }

        // set the path for the input file which contains a list of files to
        // operate over
        FileInputFormat.addInputPath(
                job, new Path(options.getPositionalArg(0)));

        TableMapReduceUtil.initTableReducerJob(
                table.tableName(), IdentityTableReducer.class, job);
        job.setNumReduceTasks(0);

        // Run the job.
        job.waitForCompletion(true);

        return 0;
    }
        
    public static class ImportCorpusMapper
            extends Mapper<ImmutableBytesWritable, Text, Text, Text> {
        
        private CorpusTable table;
        
        private DocumentReader reader;

        public void setup(Context context) {
            Configuration conf = context.getConfiguration();
            table = ReflectionUtil.getObjectInstance(conf.get(TABLE));
            reader = ReflectionUtil.getObjectInstance(conf.get(READER));
        }

        public void map(ImmutableBytesWritable key, 
                        Text value,
                        Context context) {
            table.put(reader.readDocument(value.toString()));
        }
    }
}
