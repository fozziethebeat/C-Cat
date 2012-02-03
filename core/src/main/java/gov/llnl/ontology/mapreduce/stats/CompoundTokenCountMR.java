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
import gov.llnl.ontology.mapreduce.MRArgOptions;
import gov.llnl.ontology.mapreduce.table.CorpusTable;
import gov.llnl.ontology.text.Sentence;
import gov.llnl.ontology.text.TextUtil;
import gov.llnl.ontology.util.Counter;
import gov.llnl.ontology.util.StringCounter;
import gov.llnl.ontology.util.StringPair;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edu.ucla.sspace.text.CompoundWordIterator;
import edu.ucla.sspace.util.ReflectionUtil;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.lib.reduce.IntSumReducer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * A Map/Reduce job that counts the number of occurrences for each token in a
 * corpus.
 *
 * @author Keith Stevens
 */
public class CompoundTokenCountMR extends CorpusTableMR {

    /**
     * The job description used in help text.
     */
    public static final String ABOUT =
        "Computes token counts from a particular corpus.  " +
        "If no corpus is specified, then all corpora will be used to compute " +
        "the frequencies.  The co-occurrence counts will be stored in reduce " +
        "parts on hdfs under the specified <outdir>.";

    /**
     * Runs the {@link CompoundTokenCountMR}.
     */
    public static void main(String[] args) throws Exception {
        ToolRunner.run(HBaseConfiguration.create(), 
                       new CompoundTokenCountMR(), args);
    }

    /**
     * {@inheritDoc}
     */
    protected void validateOptions(MRArgOptions options) {
        options.validate(ABOUT, "<outdir>", CompoundTokenCountMR.class,
                         1, 'C', 'c');
    }

    /**
     * {@inheritDoc}
     */
    protected String jobName() {
        return "Token Count";
    }

    /**
     * {@inheritDoc}
     */
    protected void addOptions(MRArgOptions options) {
        options.addOption('c', "compoundWordList",
                          "Specifies the compound word list.",
                          true, "FILE", "Required");
    }

    /**
     * {@inheritDoc}
     */
    protected void setupConfiguration(MRArgOptions options,
                                      Configuration conf) {
        try {
            DistributedCache.addCacheFile(
                    new URI(options.getStringOption('c')), conf);
        } catch (URISyntaxException use) {
            use.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     */
    protected Class mapperClass() {
        return CompoundTokenCountMapper.class;
    }

    /**
     * Returns the {@link Class} object for the Mapper Value of this task.
     */
    protected Class mapperKeyClass() {
        return Text.class;
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
        job.setCombinerClass(IntSumReducer.class);
        job.setReducerClass(IntSumReducer.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        TextOutputFormat.setOutputPath(
                job, new Path(options.getPositionalArg(0)));
        job.setNumReduceTasks(24);
    }

    /**
     * The {@link TableMapper} responsible for most of the work.
     */
    public static class CompoundTokenCountMapper
            extends CorpusTableMR.CorpusTableMapper<Text, IntWritable> {

        /**
         * Represents a single occurrence.
         */
        private static final IntWritable ONE = new IntWritable(1);

        private Set<String> wordList;

        /**
         * {@inheritDoc}
         */
        public void setup(Context context, Configuration conf)
                throws IOException, InterruptedException {
            Path wordListPath = DistributedCache.getLocalCacheFiles(conf)[0];
            BufferedReader br = new BufferedReader(
                    new FileReader(wordListPath.toString()));

            wordList = Sets.newHashSet();
            for (String line = null; (line = br.readLine()) != null; )
                wordList.add(line.toLowerCase());
        }

        /**
         * {@inheritDoc}
         */
        public void map(ImmutableBytesWritable key,
                        Result row, 
                        Context context)
                throws IOException, InterruptedException {
            Counter<String> counter = new StringCounter();
            for (Sentence sentence : table.sentences(row)) {
                List<String> tokens = Lists.newArrayList();
                for (StringPair tokenPos : sentence.taggedTokens())
                    if (tokenPos.x != null)
                        tokens.add(TextUtil.cleanTerm(tokenPos.x));
                Iterator<String> iter = new CompoundWordIterator(
                        tokens.iterator(), wordList);
                while (iter.hasNext())
                    counter.count(iter.next());
            }

            for (Map.Entry<String, Integer> entry : counter)
                context.write(new Text(entry.getKey()),
                              new IntWritable(entry.getValue()));

            context.getCounter("CompoundTokenCountMR", "Document").increment(1);
        }
    }
}
