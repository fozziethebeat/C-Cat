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

import gov.llnl.ontology.mapreduce.CorpusTableMR;
import gov.llnl.ontology.mapreduce.table.CorpusTable;
import gov.llnl.ontology.text.Sentence;
import gov.llnl.ontology.text.TextUtil;
import gov.llnl.ontology.util.MRArgOptions;
import gov.llnl.ontology.util.StringCounter;
import gov.llnl.ontology.util.StringPair;

import com.google.common.collect.Sets;

import edu.ucla.sspace.util.ReflectionUtil;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;


/**
 * @author Keith Stevens
 */
public class TermDocOccurrenceCountMR extends CorpusTableMR {

    public static final String ABOUT =
        "Computes a bag of words co-occurrence count between all words in a " +
        "document.  Each document co-occurrence will only be counted once " +
        "for each term pair from a particular corpus in a CorpusTable.  The " +
        "resulting counts will be stored on HDFS.";

    /**
     * Runs the {@link TokenCountMR}.
     */
    public static void main(String[] args) throws Exception {
        ToolRunner.run(HBaseConfiguration.create(),
                       new TermDocOccurrenceCountMR(), args);
    }

    /**
     * {@inheritDoc}
     */
    protected void validateOptions(MRArgOptions options) {
        options.validate(ABOUT, "<outdir>",
                         TermDocOccurrenceCountMR.class, 1, 'C', 'S');
    }


    /**
     * {@inheritDoc}
     */
    protected void addOptions(MRArgOptions options) {
        options.addOption('l', "wordList",
                          "Specifies a list of words that should be " +
                          "represented by wordsi. The format should have " +
                          "one word per line and the file should be on hdfs.",
                          true, "FILE", "Required");
    }

    /**
     * {@inheritDoc}
     */
    protected void setupConfiguration(MRArgOptions options,
                                      Configuration conf) {
        try {
            DistributedCache.addCacheFile(
                    new URI(options.getStringOption('l')), conf);
        } catch (URISyntaxException use) {
            use.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String jobName() {
        return "TermDocOccurrenceCountMR";
    }

    /**
     * {@inheritDoc}
     */
    protected Class mapperClass() {
        return TermDocOccurrenceCountMapper.class;
    }

    /**
     * Returns the {@link Class} object for the Mapper Value of this task.
     */
    protected Class mapperKeyClass() {
        return StringPair.class;
    }

    /**
     * Returns the {@link Class} object for the Mapper Value of this task.
     */
    protected Class mapperValueClass() {
        return IntWritable.class;
    }

    /**
     * Sets up the Reducer for this job.  
     */
    protected void setupReducer(String tableName,
                                Job job,
                                MRArgOptions options) {
        job.setCombinerClass(WordCountSumReducer.class);
        job.setReducerClass(WordCountSumReducer.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        TextOutputFormat.setOutputPath(
                job, new Path(options.getPositionalArg(0)));
        job.setNumReduceTasks(24);
    }

    public static class TermDocOccurrenceCountMapper
                extends CorpusTableMR.CorpusTableMapper<StringPair, IntWritable> {

        public static final IntWritable ONE = new IntWritable(1);

        private Set<String> wordList;

        /**
         * {@inheritDoc}
         */
        protected void setup(Context context, Configuration conf)
                throws IOException, InterruptedException {
            wordList = Sets.newHashSet();
            Path[] paths = DistributedCache.getLocalCacheFiles(conf);
            if (paths.length < 1)
                return;
            BufferedReader br = new BufferedReader(
                    new FileReader(paths[0].toString()));

            for (String line = null; (line = br.readLine()) != null; )
                wordList.add(line.trim().toLowerCase());
        }

        /**
         * {@inheritDoc}
         */
        public void map(ImmutableBytesWritable key,
                        Result row, 
                        Context context)
                throws IOException, InterruptedException {
            context.setStatus("Processing Docs");
            Set<String> terms = Sets.newHashSet();
            for (Sentence sentence : table.sentences(row))
                for (StringPair word : sentence.taggedTokens()) {
                    String cleanedWord = TextUtil.cleanTerm(word.x);
                    if (wordList.isEmpty() || wordList.contains(cleanedWord))
                        terms.add(cleanedWord);
                }

            for (String focus : terms) {
                context.write(new StringPair("", focus), ONE);
                for (String other : terms)
                    context.write(new StringPair(focus, other), ONE);
            }

            context.getCounter("TermDocOccurrenceCountMR", "Documents").increment(1);
        }
    }
}
