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
import gov.llnl.ontology.text.Sentence;
import gov.llnl.ontology.util.StringTriple;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edu.ucla.sspace.dependency.DependencyPath;
import edu.ucla.sspace.dependency.DependencyPathAcceptor;
import edu.ucla.sspace.dependency.DependencyRelation;
import edu.ucla.sspace.dependency.DependencyTreeNode;
import edu.ucla.sspace.dependency.FilteredDependencyIterator;
import edu.ucla.sspace.util.Counter;
import edu.ucla.sspace.util.ReflectionUtil;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * @author Keith Stevens
 */
public class StructuredWordSpaceMR extends CorpusTableMR {

    public static final String ABOUT =
        "StructuredVectorSpaceMR builds up the relational occurrences in a " +
        "dependency parsed corpus.  These will be tuples such as " +
        "(cat, OBJ, food).  Only one such tuple will be printed for an " +
        "occurrence pair, centered around the head word in the occurence.";
        
    /**
     * A prefix for any {@link Configuration} setting.
     */
    public static final String CONF_PREFIX =
        "gov.llnl.ontology.mapreduce.stats.StructuredWordSpaceMR";

    /**
     * The configuration for setting the {@link DependencyPathAcceptor}.
     */
    public static final String PATH_ACCEPTOR =
        CONF_PREFIX + ".pathAcceptor";

    /**
     * The classname of the default {@link DependencyPathAcceptor}.
     */
    public static final String DEFAULT_ACCEPTOR =
        "edu.ucla.sspace.dependency.UniversalPathAcceptor";

    public static final String MR_NAME = "StructuredWordSpaceMR";
    /**
     * Runs the {@link TokenCountMR}.
     */
    public static void main(String[] args) throws Exception {
        ToolRunner.run(HBaseConfiguration.create(),
                       new StructuredWordSpaceMR(), args);
    }

    /**
     * {@inheritDoc}
     */
    protected void validateOptions(MRArgOptions options) {
        options.validate(ABOUT, "<outdir>", StructuredWordSpaceMR.class,
                         1, 'S', 'l');
    }

    /**
     * {@inheritDoc}
     */
    public String jobName() {
        return MR_NAME;
    }

    /**
     * {@inheritDoc}
     */
    protected void addOptions(MRArgOptions options) {
        options.addOption('a', "pathAcceptor",
                          "Specifies a DependencyPathAcceptor.",
                          true, "CLASSNAME", "Optional");
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
        addToDistrubutedCache(options.getStringOption('l'), conf);
        conf.set(PATH_ACCEPTOR, options.getStringOption('a', DEFAULT_ACCEPTOR));
    }

    /**
     * {@inheritDoc}
     */
    protected Class mapperClass() {
        return StructuredWordSpaceMapper.class ;
    }

    /**
     * Returns the {@link Class} object for the Mapper Value of this task.
     */
    protected Class mapperKeyClass() {
        return StringTriple.class;
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
        job.setReducerClass(RelationTupleReducer.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        job.setNumReduceTasks(24);
        TextOutputFormat.setOutputPath(
                job, new Path(options.getPositionalArg(0)));
    }

    /**
     * The {@link TableMapper} responsible for the real work.
     */
    public static class StructuredWordSpaceMapper
            extends CorpusTableMR.CorpusTableMapper<StringTriple, IntWritable> {

        private DependencyPathAcceptor acceptor;

        private Set<String> wordList;

        /**
         * {@inheritDoc}
         */
        public void setup(Context context, Configuration conf)
                throws IOException, InterruptedException {
            acceptor = ReflectionUtil.getObjectInstance(
                    conf.get(PATH_ACCEPTOR));
            wordList = loadWordList(conf);
        }

        /**
         * {@inheritDoc}
         */
        public void map(ImmutableBytesWritable key,
                        Result row, 
                        Context context)
                throws IOException, InterruptedException {
            Counter<StringTriple> counter = new Counter<StringTriple>();
            // Iterate over each sentence and extract the dependency
            // co-occurrence statistics.
            for (Sentence sent : table.sentences(row)) {
                context.getCounter(MR_NAME, "Sentence").increment(1);

                // Iterate over each tree node and count all valid co-occurring
                // terms connected by a valid dependency path.
                for (DependencyTreeNode node : sent.dependencyParseTree()) {
                    context.getCounter(MR_NAME, "Node").increment(1);
                    if (rejectNode(node))
                        continue;
                    context.getCounter(MR_NAME, "Focus").increment(1);

                    Iterator<DependencyPath> pathIter =
                        new FilteredDependencyIterator(node, acceptor, 1);
                    while (pathIter.hasNext()) {
                        context.getCounter(MR_NAME, "Path").increment(1);

                        DependencyPath path = pathIter.next();
                        DependencyTreeNode last = path.last();
                        DependencyRelation relation = path.iterator().next();
                        if (relation.headNode() != node)
                            continue;
                        context.getCounter(MR_NAME, "Tuples").increment(1);

                        StringTriple termKey = new StringTriple(
                                node.word(), relation.relation(), last.word());
                        counter.count(termKey);
                    }
                }
            }
            for (Map.Entry<StringTriple, Integer> e : counter)
                context.write(e.getKey(), new IntWritable(e.getValue()));

            context.getCounter(MR_NAME, "Documents").increment(1);
        }

        private boolean rejectNode(DependencyTreeNode node) {
            /*
            if (!node.pos().startsWith("N") &&
                !node.pos().startsWith("V") &&
                !node.pos().startsWith("J"))
                return true;
                */
            if (!wordList.contains(node.word()))
                return true;
            return false;
        }
    }

    public static class RelationTupleReducer 
            extends Reducer<StringTriple, IntWritable, StringTriple, IntWritable> {

        public void reduce(StringTriple key,
                           Iterable<IntWritable> values,
                           Context context)
                throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable count : values)
                sum += count.get();
            context.write(key, new IntWritable(sum));
        }
    }
}
