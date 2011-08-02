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

package gov.llnl.ontology.mapreduce.stats;

import gov.llnl.ontology.mapreduce.table.CorpusTable;
import gov.llnl.ontology.text.hbase.LineDocInputFormat;
import gov.llnl.ontology.wordnet.OntologyReader;
import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.SynsetRelations;
import gov.llnl.ontology.wordnet.WordNetCorpusReader;

import edu.ucla.sspace.common.ArgOptions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;


/**
 * A Map/Reduce job that computes the shortest path between every wordnet
 * synset.
 *
 * @author Keith Stevens
 */
public class WordnetShortestPathMR extends Configured implements Tool {

    /**
     * The base name for every configuration setting.
     */
    public static final String CONF_BASE =
        "gov.llnl.ontology.mapreduce.stats.WordnetShortestPathMR";

    /**
     * The configuration setting for the wordnet directory.
     */
    public static final String WORDNET =
        CONF_BASE + ".wordnet";

    /**
     * A temp file name for the synset pairwise combinations.
     */
    public static final String TEMP_TERM_PAIR_PATH = "wordnet-pair-file";

    /**
     * Runs the {@link IngestCorpusMR}.
     */
    public static void main(String[] args) throws Exception {
        ToolRunner.run(HBaseConfiguration.create(),
                       new WordnetShortestPathMR(), args);
    }

    /**
     * {@inheritDoc}
     */
    public int run(String[] args) throws Exception {
        // Setup and valdiate the arguments.
        ArgOptions options = new ArgOptions();
        options.addOption('w', "wordnetDir",
                          "The directory path to the wordnet data files",
                          true, "PATH", "Required");

        options.parseOptions(args);
        if (!options.hasOption('w')) {
            System.err.println(
                    "usage: java WordnetShortestPathMR [OPTIONS] <outdir>\n"+
                    options.prettyPrint());
        }

        // Open the wordnet reader and gather the set of all Synsets known by
        // the ontology.
        OntologyReader reader = WordNetCorpusReader.initialize(
                options.getStringOption('w'));
        Set<Synset> synsetSet = new HashSet<Synset>();
        for (String lemma : reader.wordnetTerms())
            for (Synset synset : reader.getSynsets(lemma))
                synsetSet.add(synset);

        // Compute each pairing of Synsets and write that pairing to a file in
        // HDFS.
        Synset[] synsets = synsetSet.toArray(new Synset[0]);
        PrintStream outStream = createPrintStream();
        for (int i = 0; i < synsets.length; ++i)
            for (int j = i+1; j < synsets.length; ++j)
                outStream.printf("%s|%s\n",
                                 synsets[i].getName(), synsets[j].getName());
        outStream.close();

        // Store the wordnet directory information so that the mappers can load
        // it up.  They need it to figure out the shortest path information.
        Configuration conf = getConf();
        conf.set(WORDNET, options.getStringOption('w'));

        // Setup the job information.
        Job job = new Job(conf, "Compute Wordnet Shortest Paths");
        job.setJarByClass(WordnetShortestPathMR.class);

        job.setMapperClass(WordnetShortestPathMapper.class);

        // The input file will be the temporary file created with the synset
        // pairings.
        job.setInputFormatClass(LineDocInputFormat.class);
        FileInputFormat.addInputPath(job, new Path(TEMP_TERM_PAIR_PATH));

        // The mappers do all of the real work, so we just write their output
        // straight to disk.
        job.setCombinerClass(Reducer.class);
        job.setReducerClass(Reducer.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        TextOutputFormat.setOutputPath(
                job, new Path(options.getPositionalArg(0)));

        // Start the job.
        job.waitForCompletion(true);

        return 0;
    }

    /**
     * Creates a new temporary file on HDFS that will store the {@link Synset}
     * name pairs to be used for finding the shortest distance between all
     * nodes.
     */
    private static PrintStream createPrintStream() throws Exception {
        Configuration conf = new Configuration();
        FileSystem fs = FileSystem.get(conf);
        Path finalPath = new Path(TEMP_TERM_PAIR_PATH);
        FSDataOutputStream ostr = fs.create(finalPath);
        return new PrintStream(ostr);
    }

    /**
     * The {@link Mapper} responsible for the real work.
     */
    public static class WordnetShortestPathMapper
            extends Mapper<LongWritable, Text, Text, IntWritable> {

        /**
         * The {@link OntologyReader} needed for accessing any {@link Synset}.
         */
        private OntologyReader reader;

        /**
         * {@inheritDoc}
         */
        public void setup(Context context)
                throws IOException, InterruptedException {
            Configuration conf = context.getConfiguration();
            reader = WordNetCorpusReader.initialize(conf.get(WORDNET));
        }

        /**
         * {@inheritDoc}
         */
        public void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException {
            String[] terms = value.toString().split("\\|");
            Synset synset1 = reader.getSynset(terms[0]);
            Synset synset2 = reader.getSynset(terms[1]);
            int shortestPath = SynsetRelations.shortestPathDistance(
                    synset1, synset2);
            context.write(value, new IntWritable(shortestPath));
        }
    }
}
