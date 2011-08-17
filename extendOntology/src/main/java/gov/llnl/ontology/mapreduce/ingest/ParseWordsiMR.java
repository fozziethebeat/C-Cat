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

import gov.llnl.ontology.text.parse.MaltLinearParser;
import gov.llnl.ontology.text.parse.Parser;
import gov.llnl.ontology.util.MRArgOptions;

import com.google.common.collect.Lists;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.util.List;


/**
 * @author Keith Stevens
 */
public class ParseWordsiMR extends Configured implements Tool {

    /**
     * Acquire the logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(ParseWordsiMR.class);

    /**
     * Runs the {@link IngestCorpusMR}.
     */
    public static void main(String[] args) throws Exception {
        ToolRunner.run(HBaseConfiguration.create(), new ParseWordsiMR(), args);
    }

    /**
     * {@inheritDoc}
     */
    public int run(String[] args)
            throws Exception, InterruptedException, ClassNotFoundException {
        // Setup the main arguments used.
        MRArgOptions options = new MRArgOptions();

        LOG.info("Parse Options");
        // Parse and validate the arguments.
        options.parseOptions(args);
        options.validate("", "<indir>+", ParseWordsiMR.class, -1);

        LOG.info("Setup Configuration");
        // Setup the configuration so that the mappers know which classes to
        // use.
        Configuration conf = getConf();

        LOG.info("Setup Input Data Format");
        // Create the job and set the jar.
        Job job = new Job(conf, "Parse For Wordsi");
        job.setJarByClass(ParseWordsiMR.class);
        job.setMapperClass(ParseWordsiMapper.class);

        // Set the input format class based on the option given.
        job.setInputFormatClass(TextInputFormat.class);

        LOG.info("Setup Mapper");
        // Set the path for the input file which contains a list of files to
        // operate over
        for (int i = 0; i < options.numPositionalArgs(); ++i)
            FileInputFormat.addInputPath(
                    job, new Path(options.getPositionalArg(i)));

        job.setOutputFormatClass(TextOutputFormat.class);
        TextOutputFormat.setOutputPath(
                job, new Path(options.getPositionalArg(0)));
        job.setNumReduceTasks(0);

        LOG.info("Start ParseWordsiMR job"); 
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
    public static class ParseWordsiMapper 
            extends Mapper<ImmutableBytesWritable, Text, Text, Text> {
        
        private Parser parser;

        public void setup(Context context) {
            parser = new MaltLinearParser();
        }

        /**
         * {@inheritDoc}
         */
        public void map(ImmutableBytesWritable key, 
                        Text value,
                        Context context)
                throws IOException, InterruptedException {
            String[] headerSpanText = value.toString().split("\\s+", 3);
            String header = headerSpanText[0];
            String[] startEnd = headerSpanText[1].split(",");
            int start = Integer.parseInt(startEnd[0]);
            int end = Integer.parseInt(startEnd[1]);

            String keyWord = headerSpanText[2].substring(start, end);
            String parsedText = parser.parseText(null, headerSpanText[2]);
            parsedText = parsedText.replaceAll("\n", "||");

            context.write(new Text(keyWord), new Text(parsedText));
            context.getCounter("ParseWordsiMR", "Documents").increment(1);
        }
    }
}

