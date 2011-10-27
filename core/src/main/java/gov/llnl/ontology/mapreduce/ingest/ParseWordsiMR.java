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

import gov.llnl.ontology.mapreduce.MRArgOptions;
import gov.llnl.ontology.text.parse.MaltLinearParser;
import gov.llnl.ontology.text.parse.Parser;
import gov.llnl.ontology.text.tag.OpenNlpMEPOSTagger;
import gov.llnl.ontology.text.tokenize.OpenNlpMETokenizer;
import gov.llnl.ontology.util.StringPair;

import opennlp.tools.postag.POSTagger;
import opennlp.tools.tokenize.Tokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;


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
        for (int i = 1; i < options.numPositionalArgs(); ++i)
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
            extends Mapper<LongWritable, Text, Text, Text> {
        
        private Parser parser;

        private Tokenizer tokenizer;

        private POSTagger tagger;
        public void setup(Context context) {
            parser = new MaltLinearParser();
            tokenizer = new OpenNlpMETokenizer();
            tagger = new OpenNlpMEPOSTagger();
        }

        /**
         * {@inheritDoc}
         */
        public void map(LongWritable key, 
                        Text value,
                        Context context)
                throws IOException, InterruptedException {
            // Split the context to get the header text, the indices for the
            // focus word, and the context itself.  We will replace the focus
            // word with the header, and for the header we replace any spces with
            // underscoresso that whitespace tokenization picks up the word of
            // interest.
            String[] headerSpanText = value.toString().split("::", 4);
            String header = headerSpanText[0].replaceAll("\\s+", "_");
            int start = Integer.parseInt(headerSpanText[1]);
            int end = Integer.parseInt(headerSpanText[2]);

            // Pre-tokenize and tag the sentence.  We must do for the text
            // before the header and after the header seperately.  If we
            // tokenize with the header in the sentence, the tokenizer is likely
            // to split the word on the _ markers, which are needed to match the
            // focus word with the header in each context.

            // Tokenize and tag the previous and post text.
            String preText = headerSpanText[3].substring(0, start);
            String[] preTokens = tokenizer.tokenize(preText);
            String[] preTags = tagger.tag(preTokens);

            String postText = headerSpanText[3].substring(end+1);
            String[] postTokens = tokenizer.tokenize(postText);
            String[] postTags = tagger.tag(postTokens);

            StringPair[] pairs = new StringPair[
                preTokens.length + 1 + postTokens.length];

            // Add in the pre text.
            for (int i = 0; i < preTokens.length; ++i)
                pairs[i] = new StringPair(preTokens[i], preTags[i]);

            // Add in the header.  Headers for this experiement will always be
            // nouns, so we just give it NN by default.
            pairs[preTokens.length] = new StringPair(header, "NN");

            // Add in the post text.
            for (int i = 0, j = preTokens.length+1; i < postTokens.length; 
                    ++i, ++j)
                pairs[j] = new StringPair(postTokens[i], postTags[i]);

            // Parse the text.
            String parsedText = parser.parseText(null, pairs);
            //parsedText = parsedText.replaceAll("\n", "||");

            context.write(new Text(header), new Text("\n"+parsedText+"\n"));
            context.getCounter("ParseWordsiMR", "Documents").increment(1);
        }
    }
}

