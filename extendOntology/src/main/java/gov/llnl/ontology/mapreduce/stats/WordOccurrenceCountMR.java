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
import gov.llnl.ontology.util.AnnotationUtil;
import gov.llnl.ontology.util.StringCounter;
import gov.llnl.ontology.util.StringPair;
import gov.llnl.ontology.util.MRArgOptions;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import edu.stanford.nlp.pipeline.Annotation;

import edu.ucla.sspace.util.ReflectionUtil;
import edu.ucla.sspace.util.CombinedIterator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;


/**
 * A Map/Reduce job that counts word co-occurrence frequencies and writes them
 * to hdfs.
 *
 * @author Keith Stevens
 */
public class WordOccurrenceCountMR extends CorpusTableMR {

    /**
     * The job description used in the help text.
     */
    public static final String ABOUT =
        "Computes word co-occurrence frequencies from a particular corpus.  " +
        "If no corpus is specified, then all corpora will be used to compute " +
        "the frequencies.  The co-occurrence counts will be stored in reduce " +
        "parts on hdfs under the specified <outdir>.";

    /**
     * The prefix for every configuration.
     */
    public static final String CONF_PREFIX =
        "gov.llnl.ontology.mapreduce.stats.WordOccurrenceCountMR";

    /**
     * The configuration set when part of speech features should be used.
     */
    public static final String USE_POS =
        CONF_PREFIX + ".usePos";

    /**
     * The configuration set when word ordering features should be used.
     */
    public static final String USE_ORDER =
        CONF_PREFIX + ".useOrder";

    /**
     * The configuration used to set the maximum sliding window size.
     */
    public static final String WINDOW_SIZE=
        CONF_PREFIX + ".windowSize";

    /**
     * Runs the {@link WordOccurrenceCountMR}.
     */
    public static void main(String[] args) throws Exception {
        ToolRunner.run(HBaseConfiguration.create(), new WordOccurrenceCountMR(), args);
    }

    /**
     * {@inheritDoc}
     */
    protected String jobName() {
        return "Word Co-Occurrence Count";
    }

    /**
     * {@inheritDoc}
     */
    protected void addOptions(MRArgOptions options) {
        options.addOption('w', "windowSize",
                          "Sets the window size used for considering word " +
                          "co-occurrences. (Default: 10)",
                          true, "INT", "Optional");
        options.addOption('o', "useOrdering",
                          "Set to true if word ordering should be included " +
                          "in each co-occurrence feature",
                          false, null, "Optional");
        options.addOption('p', "usePartOfSpeech",
                          "Set to true if parts of speech should be included " +
                          "in each co-occurrence feature",
                          false, null, "Optional");
    }

    /**
     * {@inheritDoc}
     */
    protected void validateOptions(MRArgOptions options) {
        options.validate(ABOUT, "<outdir>", WordOccurrenceCountMR.class,
                         1, 'C');
    }

    /**
     * {@inheritDoc}
     */
    protected void setupConfiguration(MRArgOptions options,
                                      Configuration conf) {
        conf.set(WINDOW_SIZE, options.getStringOption('w', "10"));
        if (options.hasOption('o'))
            conf.set(USE_ORDER, "T");
        if (options.hasOption('p'))
            conf.set(USE_POS, "T");
    }

    /**
     * {@inheritDoc}
     */
    protected Class mapperClass() {
        return WordOccurrenceCountMapper.class;
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

    /**
     * The {@link TableMapper} responsible for most of the work.
     */
    public static class WordOccurrenceCountMapper 
            extends CorpusTableMR.CorpusTableMapper<StringPair, IntWritable> {

        /**
         * The sliding window size.
         */
        private int windowSize;

        /**
         * Set to true when part of speech features should be included.
         */
        private boolean usePos;

        /**
         * Set to true when word ordering features should be included.
         */
        private boolean useOrdering;

        /**
         * {@inheritDoc}
         */
        public void setup(Context context, Configuration conf)
                throws IOException, InterruptedException {
            windowSize = Integer.parseInt(conf.get(WINDOW_SIZE));
            usePos = conf.get(USE_POS) != null;
            useOrdering = conf.get(USE_ORDER) != null;
        }

        /**
         * {@inheritDoc}
         */
        public void map(ImmutableBytesWritable key,
                        Result row, 
                        Context context)
                throws IOException, InterruptedException {
            // Merge all of the annotation iterators into one.
            List<Iterator<Annotation>> tokenIters = Lists.newArrayList();
            for (Sentence sentence : table.sentences(row))
                tokenIters.add(sentence.iterator());
            Iterator<Annotation> tokens = new CombinedIterator<Annotation>(
                    tokenIters);

            // Initialize the local counts.
            Map<String, StringCounter> wocCounts = Maps.newHashMap();

            // Create the previous and next windows.
            Queue<Annotation> prev = new ArrayDeque<Annotation>();
            Queue<Annotation> next = new ArrayDeque<Annotation>();

            // Fill the next window.
            while (tokens.hasNext() && next.size() < windowSize)
                next.offer(tokens.next());

            // Iterate through each available word and count the co-occurrences.
            while (!next.isEmpty()) {
                Annotation focus = next.remove();

                // Add the next available token to the next queue.
                if (tokens.hasNext())
                    next.offer(tokens.next());

                // Skipp empty focus words.
                String focusWord = AnnotationUtil.word(focus);
                if (focusWord == null)
                    continue;

                // Get the counter for the focus word.
                StringCounter counts = wocCounts.get(focusWord);
                if (counts == null) {
                    counts = new StringCounter();
                    wocCounts.put(focusWord, counts);
                }

                //  Count the co-occurrences in with the previous and next
                //  words.
                addContextTerms(counts, prev, -1 * prev.size());
                addContextTerms(counts, next, 1);

                // Shift the focus token to the prev queue and remove any old
                // tokens if needed.
                prev.offer(focus);
                if (prev.size() > windowSize)
                    prev.remove();
                context.getCounter("WordOccurrenceCountMR", "Focus Word").increment(1);
            }

            WordCountSumReducer.emitCounts(wocCounts, context);
            context.getCounter("WordOccurrenceCountMR", "Documents").increment(1);
        }

        /**
         * Adds a count for each word feature in {@code words} to {@code count}.  If
         * {@code usePos} is true, the feature will be the word plus the part of
         * speech.  If {@code useOrdering} is true, the feature will be the word
         * plus the distance, positive or negative, from the focus word.
         */
        protected void addContextTerms(StringCounter  counts,
                                       Queue<Annotation> words,
                                       int distance)
                throws IOException, InterruptedException {
            // Decrement the distance once so that we can always add to it at
            // the start of the loop and ensure it's update correctly regardless of
            // the code path.
            --distance;

            // Iterate through each of the context words.
            for (Annotation term : words) {
                // Modify the distance.
                ++distance;

                // Get the word and part of speech.  Skip any that are null.
                String word = AnnotationUtil.word(term);
                String pos = AnnotationUtil.pos(term);
                if (word == null || pos == null)
                    continue;

                // Modify the feature if needed and add the count.
                if (usePos) 
                    counts.count(word + "-" + pos);
                else if (useOrdering) 
                    counts.count(word + "-" + distance);
                else
                    counts.count(word);
            }
        }
    }
}
