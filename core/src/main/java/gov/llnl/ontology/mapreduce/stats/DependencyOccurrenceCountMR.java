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
import gov.llnl.ontology.util.Counter;
import gov.llnl.ontology.util.MRArgOptions;

import com.google.common.collect.Maps;

import edu.ucla.sspace.dependency.FilteredDependencyIterator;
import edu.ucla.sspace.dependency.DependencyPathAcceptor;
import edu.ucla.sspace.dependency.DependencyPath;
import edu.ucla.sspace.dependency.DependencyRelation;
import edu.ucla.sspace.dependency.DependencyTreeNode;

import edu.ucla.sspace.util.ReflectionUtil;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;


/**
 * A MapReduce task that counts the number of co-occurrences in a sentence that
 * are connected by a valid {@link DependencyPath}.  Paths will be validated by
 * both their length and a {@link DependencyPathAcceptor}.  The final word in
 * each path, and, optionally, the final relation may be included, as the
 * co-occurrence feature.  This mapper pairs with the {@link
 * WordCountSumReducer}.
 *
 * @author Keith Stevens
 */
public class DependencyOccurrenceCountMR extends CorpusTableMR {

    /**
     * The about string.
     */
    public static final String ABOUT =
        "Computes the co-occurrence frequencies between words that are " +
        "connected by dependency paths.  Dependency paths will be valided " +
        "by a DependencyPathAcceptor and must be shorter than a finite " +
        "path length.  The final word in each dependency path will be " +
        "the co-occurring word with the root of the path, and the " +
        "final dependency connecting them may also be included in the " +
        "word form.  If no corpus is specified, then all corpora will be " +
        "used to compute the frequencies.  The co-occurrence counts will be " +
        "stored in reduce parts on hdfs under the specified <outdir>.";

    /**
     * A prefix for any {@link Configuration} setting.
     */
    public static final String CONF_PREFIX =
        "gov.llnl.ontology.mapreduce.stats.DependencyOccurrenceCountMR";

    /**
     * The configuration for setting the {@link DependencyPathAcceptor}.
     */
    public static final String PATH_ACCEPTOR =
        CONF_PREFIX + ".pathAcceptor";

    /**
     * The configuration for setting the path length.
     */
    public static final String PATH_LENGTH =
        CONF_PREFIX + ".pathLength";

    /**
     * The configuration for setting whether or not relations should be in the
     * co-occurrence features.
     */
    public static final String USE_RELATION =
        CONF_PREFIX + ".useRelation";

    /**
     * The classname of the default {@link DependencyPathAcceptor}.
     */
    public static final String DEFAULT_ACCEPTOR =
        "edu.ucla.sspace.dependency.UniversalAcceptor";

    /**
     * Runs the {@link DependencyOccurrenceCountMR}.
     */
    public static void main(String[] args) throws Exception {
        ToolRunner.run(HBaseConfiguration.create(),
                       new DependencyOccurrenceCountMR(), args);
    }

    /**
     * {@inheritDoc}
     */
    protected String jobName() {
        return "Dependency Co-Occurrence Count";
    }

    /**
     * {@inheritDoc}
     */
    protected void addOptions(MRArgOptions options) {
        options.addOption('l', "pathLength",
                          "Sets the Path Length used for considering word " +
                          "co-occurrences. (Default: 10)",
                          true, "INT", "Optional");
        options.addOption('a', "pathAcceptor",
                          "Sets the PathAcceptor used for validating " +
                          "paths. (Default: UniversalAcceptor)",
                          true, "CLASSNAME", "Optional");
        options.addOption('r', "useRelation",
                          "Set to true if the last relation in the " +
                          "dependency path should be included in the " +
                          "feature form",
                          false, null, "Optional");
    }

    /**
     * {@inheritDoc}
     */
    protected void validateOptions(MRArgOptions options) {
        options.validate(ABOUT, "<outdir>", DependencyOccurrenceCountMR.class,
                         1, 'C');
    }

    /**
     * {@inheritDoc}
     */
    protected void setupConfiguration(MRArgOptions options,
                                      Configuration conf) {
        conf.set(PATH_LENGTH, options.getStringOption('l', "10"));
        conf.set(PATH_ACCEPTOR, options.getStringOption(
                    'a', DEFAULT_ACCEPTOR));
        if (options.hasOption('r'))
            conf.set(USE_RELATION, "T");
    }

    /**
     * {@inheritDoc}
     */
    protected Class mapperClass() {
        return DependencyOccurrenceCountMapper.class;
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
        return Text.class;
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
     * Iterates through each {@link DependencyTreeNode} and finds all valid
     * paths from that node to other nodes in the sentence.  Each co-occurrence
     * is emitted with it's total count in the whole document.
     */
    public static class DependencyOccurrenceCountMapper 
            extends TableMapper<Text, Text> {

        /**
         * The {@link CorpusTable} that accesses documents.
         */
        private CorpusTable table;

        /**
         * The maximum valid path length.
         */
        private int pathLength;

        /**
         * Set to true if the final {@link DependencyRelation} should be
         * included in co-occurrence features.
         */
        private boolean useRelation;

        /**
         * The {@link DependencyPathAcceptor} that validates paths from a node.
         */
        private DependencyPathAcceptor acceptor;

        /**
         * {@inheritDoc}
         */
        public void setup(Context context)
                throws IOException, InterruptedException {
            Configuration conf = context.getConfiguration();
            table = ReflectionUtil.getObjectInstance(conf.get(TABLE));
            table.table();
            pathLength = Integer.parseInt(conf.get(PATH_LENGTH));
            acceptor = ReflectionUtil.getObjectInstance(
                    conf.get(PATH_ACCEPTOR));
            useRelation = conf.get(USE_RELATION) != null;
        }

        /**
         * {@inheritDoc}
         */
        public void map(ImmutableBytesWritable key,
                        Result row, 
                        Context context)
                throws IOException, InterruptedException {
            // Setup a local count of co-occurrences in this document.  We do
            // this to try and reduce the amount of data written from the map to
            // the reducer.
            Map<String, Counter<String>> wocCounts = Maps.newHashMap();

            // Iterate over each sentence and extract the dependency
            // co-occurrence statistics.
            for (Sentence sentence : table.sentences(row)) {
                // Extract the dependency tree for this sentence.
                DependencyTreeNode[] tree = sentence.dependencyParseTree();
                // Iterate over each tree node and count all valid co-occurring
                // terms connected by a valid dependency path.
                for (DependencyTreeNode focus : tree) {
                    // Get the counts for this focus word.
                    Counter<String> counts = wocCounts.get(focus.word());
                    if (counts == null) {
                        counts = new Counter<String>();
                        wocCounts.put(focus.word(), counts);
                    }

                    // Iterate over the paths starting at this node.
                    Iterator<DependencyPath> pathIter =
                        new FilteredDependencyIterator(
                                focus, acceptor, pathLength);
                    while (pathIter.hasNext()) {
                        // Add a co-occurrence count for this dependency path.
                        DependencyPath path = pathIter.next();
                        DependencyTreeNode last = path.last();
                        DependencyRelation relation = path.lastRelation();
                        counts.count((useRelation) 
                            ? last.word() + "-" + relation.relation()
                            : last.word());
                    }
                }
            }

            // Emit the co-occurrence counts found for this document.
            WordCountSumReducer.emitCounts(wocCounts, context);
        }
    }
}
