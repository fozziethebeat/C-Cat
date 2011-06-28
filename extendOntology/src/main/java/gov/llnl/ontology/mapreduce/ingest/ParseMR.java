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

import gov.llnl.ontology.text.Parser;
import gov.llnl.ontology.text.Sentence;

import gov.llnl.ontology.util.MRArgOptions;

import edu.stanford.nlp.ling.CoreAnnotations.CoNLLDepTypeAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CoNLLDepParentIndexAnnotation;

import edu.stanford.nlp.pipeline.Annotation;

import edu.ucla.sspace.util.ReflectionUtil;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;

import org.apache.hadoop.mapreduce.Job;

import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import org.apache.hadoop.hbase.HBaseConfiguration;

import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;

import org.apache.hadoop.hbase.io.ImmutableBytesWritable;

import org.apache.hadoop.hbase.mapreduce.IdentityTableReducer;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;

import java.io.IOException;

import java.util.Iterator;
import java.util.List;


/**
 * @author Keith Stevens
 */
public class ParseMR extends Configured implements Tool {

    /**
     * The configuration key prefix.
     */
    public static String CONF_PREFIX =
        "gov.llnl.ontology.mapreduce.ingest.ParseMR";

    /**
     * The configuration key for setting the {@link CorpusTable}.
     */
    public static String TABLE =
        CONF_PREFIX + ".corpusTable";

    /**
     * The configuration key for setting the {@link Tokenizer}.
     */
    public static String PARSER =
        CONF_PREFIX + ".parser";

    /**
     * Runs the {@link IngestCorpusMR}.
     */
    public static void main(String[] args) throws Exception {
        ToolRunner.run(new Configuration(), new IngestCorpusMR(), args);
    }

    /**
     * {@inheritDoc}
     */
    public int run(String[] args)
            throws IOException, InterruptedException, ClassNotFoundException {
        // Setup the main arguments used.
        MRArgOptions options = new MRArgOptions();
        options.addOption('p', "parser",
                          "Specifies the Parser to use for " +
                          "dependency parsing sentences.",
                          true, "CLASSNAME", "Required");

        // Parse and validate the arguments.
        options.parseOptions(args);
        if (!options.hasOption('p')) {
            System.err.println("usage: java IngestCorpusMR [OPTIONS]\n" +
                               options.prettyPrint());
            System.exit(1);
        }

        // Setup the configuration so that the mappers know which classes to
        // use.
        Configuration conf = new HBaseConfiguration();
        conf.set(TABLE, options.corpusTableType());
        conf.set(PARSER, options.getStringOption('p'));

        // Create the corpus table and setup the scan.
        CorpusTable table = options.corpusTable();
        Scan scan = new Scan();
        table.setupScan(scan, options.sourceCorpus(), false);

        // Create the job and set the jar.
        Job job = new Job(conf, "Parse Corpus");
        job.setJarByClass(ParseMR.class);

        // Setup the mapper class.
        TableMapReduceUtil.initTableMapperJob(table.tableName(),
                                              scan, 
                                              ParseMapper.class,
                                              ImmutableBytesWritable.class,
                                              Put.class,
                                              job);

        // Setup an empty reducer.
        TableMapReduceUtil.initTableReducerJob(table.tableName(), 
                                               IdentityTableReducer.class, 
                                               job);
        job.setNumReduceTasks(0);

        // Run the job.
        job.waitForCompletion(true);

        return 0;
    }

    public class ParseMapper 
            extends TableMapper<ImmutableBytesWritable, Put> {

        /**
         * The {@link CorpusTable} that dictates the structure of the table
         * containing a corpus.
         */
        private CorpusTable table;

        /**
         * The {@link Parser} responsible for dependency parsing sentences.
         */
        private Parser parser;

        /**
         * {@inheritDoc}
         */
        public void setup(Context context) {
            Configuration conf = context.getConfiguration();
            table = ReflectionUtil.getObjectInstance(conf.get(TABLE));
            parser = ReflectionUtil.getObjectInstance(conf.get(PARSER));
        }

        /**
         * {@inheritDoc}
         */
        public void map(ImmutableBytesWritable key,
                        Result row, 
                        Context context) {
            // Reject any rows that should not be processed.
            if (!table.shouldProcessRow(row))
                return;

            List<Sentence> sentences = table.sentences(row);
            for (Sentence sentence : sentences) {
                String parsedSentence = parser.parseText(
                        "", sentence.taggedTokens());

                Iterator<Annotation> tokens = sentence.iterator();
                for (String line : parsedSentence.split("\\n+")) {
                    Annotation token = tokens.next();
                    String[] toks = line.split("\\s+");

                    token.set(CoNLLDepParentIndexAnnotation.class,
                              Integer.parseInt(toks[6]));
                    token.set(CoNLLDepTypeAnnotation.class, toks[7]);
                }
            }

            // Add the list of Sentence annotations.
            table.put(key, sentences);
        }
    }
}
