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
import gov.llnl.ontology.text.DependencyWordBasisMapping;
import gov.llnl.ontology.text.DependencyRelationBasisMapping;
import gov.llnl.ontology.text.Sentence;
import gov.llnl.ontology.text.StringBasisMapping;
import gov.llnl.ontology.util.StringCounter;
import gov.llnl.ontology.util.MRArgOptions;
import gov.llnl.ontology.util.StringPair;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edu.ucla.sspace.basis.BasisMapping;
import edu.ucla.sspace.dependency.DependencyPath;
import edu.ucla.sspace.dependency.DependencyPathAcceptor;
import edu.ucla.sspace.dependency.DependencyPathWeight;
import edu.ucla.sspace.dependency.DependencyTreeNode;
import edu.ucla.sspace.dependency.FilteredDependencyIterator;
import edu.ucla.sspace.dv.DependencyPathBasisMapping;
import edu.ucla.sspace.util.ReflectionUtil;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.wordsi.DependencyContextGenerator;
import edu.ucla.sspace.wordsi.OccurrenceDependencyContextGenerator;
import edu.ucla.sspace.wordsi.OrderingDependencyContextGenerator;
import edu.ucla.sspace.wordsi.PartOfSpeechDependencyContextGenerator;
import edu.ucla.sspace.wordsi.WordOccrrenceDependencyContextGenerator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.ToolRunner;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

/**
 * @author Keith Stevens
 */
public class WordsiMR extends CorpusTableMR {

    public static final String ABOUT =
        "Computes context vectors for a wordsi model using a particular " +
        "DependencyContextGenerator.  The computed context vectors will be in a pure " +
        "text format with the context id as the first token and then a list of " +
        "feature,count pairs.  The full form of each feature will be written, rather " +
        "than an index matched to each feature since mappers are unable to " +
        "coordinate feature index mappings.";

    /**
     * A prefix for any {@link Configuration} setting.
     */
    public static final String CONF_PREFIX =
        "gov.llnl.ontology.mapreduce.stats.WordsiMR";

    /**
     * The configuration for setting the {@link DependencyPathAcceptor}.
     */
    public static final String PATH_ACCEPTOR =
        CONF_PREFIX + ".pathAcceptor";

    /**
     * The configuration for setting the {@link DependencyPathWeight}.
     */
    public static final String PATH_WEIGHT =
        CONF_PREFIX + ".pathWeight";

    /**
     * The configuration for setting the path length or word co-occurrence
     * window.
     */
    public static final String PATH_LENGTH =
        CONF_PREFIX + ".pathLength";

    /**
     * The configuration for setting the {@link DependencyPathBasisMapping}.
     */
    public static final String DEPENDENCY_BASIS=
        CONF_PREFIX + ".dependencyBasis";

    /**
     * The configuration set when part of speech features should be used.
     */
    public static final String USE_POS =
        CONF_PREFIX + ".usePos";

    /**
     * The configuration set when word ordering features should be used.
     */
    public static final String USE_ORDERING =
        CONF_PREFIX + ".useOrdering";

    /**
     * The classname of the default {@link DependencyPathAcceptor}.
     */
    public static final String DEFAULT_ACCEPTOR =
        "edu.ucla.sspace.dependency.UniversalPathAcceptor";

    /**
     * The classname of the default {@link DependencyPathWeight}.
     */
    public static final String DEFAULT_WEIGHT =
        "edu.ucla.sspace.dependency.FlatPathWeight";

    /**
     * Set to true if dependency features should be used instead of just
     * co-occurrence features.
     */
    private boolean useDependency;

    /**
     * Runs the {@link TokenCountMR}.
     */
    public static void main(String[] args) throws Exception {
        ToolRunner.run(HBaseConfiguration.create(), new WordsiMR(), args);
    }

    /**
     * {@inheritDoc}
     */
    protected void validateOptions(MRArgOptions options) {
        options.validate(ABOUT, "<outdir>", WordsiMR.class, 1,
                         'C', 'S', 'w', 'l');
    }

    /**
     * {@inheritDoc}
     */
    public String jobName() {
        return "WordsiMR";
    }

    /**
     * {@inheritDoc}
     */
    protected void addOptions(MRArgOptions options) {
        options.addOption('w', "windowSize",
                          "Specifies the maximum window size in a sliding " +
                          "co-occurrence window, if using word co-occurrence " +
                          "features.  If using dependency path features, " +
                          "this specifies the maximum path length of a valid " +
                          "path.",
                          true, "INT", "Required");
        options.addOption('p', "usePartOfSpeech",
                          "Set if part of speech features should be used in " +
                          "a word co-occurrence model",
                          false, null, "At most one of");
        options.addOption('o', "useOrdering",
                          "Set if word ordering features should be used in " +
                          "a word co-occurrence model",
                          false, null, "At most one of");
        options.addOption('d', "dependencyBasisMapping",
                          "Sets the dependency basis mapping for dependency " +
                          "co-occurrence features.",
                          true, "CLASSNAME", "At most one of");
        options.addOption('a', "pathAcceptor",
                          "Sets the dependency path acceptor used to " +
                          "validate paths.",
                          true, "CLASSNAME", "Optional");
        options.addOption('W', "pathWeight",
                          "Sets the dependency path weight applied to " +
                          "valid paths.",
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
        try {
            DistributedCache.addCacheFile(
                    new URI(options.getStringOption('l')), conf);
        } catch (URISyntaxException use) {
            use.printStackTrace();
            System.exit(1);
        }

        useDependency = false;
        conf.set(PATH_LENGTH, options.getStringOption('w'));
        if (options.hasOption('p'))
            conf.set(USE_POS, "true");
        else if (options.hasOption('o'))
            conf.set(USE_ORDERING, "true");
        else if (options.hasOption('d')) {
            useDependency = true;
            conf.set(DEPENDENCY_BASIS, options.getStringOption('d'));
            conf.set(PATH_ACCEPTOR,
                     options.getStringOption('a', DEFAULT_ACCEPTOR));
            conf.set(PATH_WEIGHT,
                     options.getStringOption('W', DEFAULT_WEIGHT));
        } 
    }

    /**
     * {@inheritDoc}
     */
    protected Class mapperClass() {
        return (useDependency)
            ? WordsiDependencyMapper.class 
            : WordsiOccurrenceMapper.class;
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
        job.setOutputFormatClass(TextOutputFormat.class);
        TextOutputFormat.setOutputPath(
                job, new Path(options.getPositionalArg(0)));
    }

    /**
     * The {@link TableMapper} responsible for the real work.
     */
    public static class WordsiDependencyMapper
            extends CorpusTableMR.CorpusTableMapper<Text, Text> {

        private DependencyContextGenerator generator;

        private DependencyPathBasisMapping basis;

        private Set<String> wordList;

        /**
         * {@inheritDoc}
         */
        public void setup(Context context, Configuration conf)
                throws IOException, InterruptedException {
            int windowSize = Integer.parseInt(conf.get(PATH_LENGTH));
            basis =
                ReflectionUtil.getObjectInstance(conf.get(DEPENDENCY_BASIS));
            DependencyPathAcceptor acceptor = 
                ReflectionUtil.getObjectInstance(conf.get(PATH_ACCEPTOR));
            DependencyPathWeight weight = 
                ReflectionUtil.getObjectInstance(conf.get(PATH_WEIGHT));
            generator = new WordOccrrenceDependencyContextGenerator(
                    basis, weight, acceptor, windowSize);

            Path wordListPath = DistributedCache.getLocalCacheFiles(conf)[0];
            BufferedReader br = new BufferedReader(
                    new FileReader(wordListPath.toString()));

            wordList = Sets.newHashSet();
            for (String line = null; (line = br.readLine()) != null; )
                wordList.add(line);
            System.out.printf("Num valid words: %d\n", wordList.size());
        }

        /**
         * {@inheritDoc}
         */
        public void map(ImmutableBytesWritable key,
                        Result row, 
                        Context context)
                throws IOException, InterruptedException {
            // Iterate over each sentence and extract the dependency
            // co-occurrence statistics.
            for (Sentence sentence : table.sentences(row)) {
                // Extract the dependency tree for this sentence.
                DependencyTreeNode[] tree = sentence.dependencyParseTree();
                // Iterate over each tree node and count all valid co-occurring
                // terms connected by a valid dependency path.
                for (int i = 0; i < tree.length; ++i) {
                    if (!wordList.contains(tree[i].word()))
                        continue;
                    SparseDoubleVector vector = generator.generateContext(
                            tree, i);
                    emitContext(tree[i].word(), basis, vector, context);
                    context.getCounter("WordsiMR", "Contexts").increment(1);
                }
            }
        }
    }

    /**
     * The {@link TableMapper} responsible for the real work.
     */
    public static class WordsiOccurrenceMapper
            extends CorpusTableMR.CorpusTableMapper<Text, Text> {

        private DependencyContextGenerator generator;

        private BasisMapping<String, String> basis;

        private Set<String> wordList;

        /**
         * {@inheritDoc}
         */
        protected void setup(Context context, Configuration conf)
                throws IOException, InterruptedException {
            basis = new StringBasisMapping();
            int windowSize = Integer.parseInt(conf.get(PATH_LENGTH));
            if (conf.get(USE_POS) != null) {
                generator = new PartOfSpeechDependencyContextGenerator(
                        basis, windowSize);
                System.err.println("USING POS");
            }
            else if (conf.get(USE_ORDERING) != null) {
                generator = new OrderingDependencyContextGenerator(
                        basis, windowSize);
                System.err.println("USING order");
            }
            else {
                generator = new OccurrenceDependencyContextGenerator(
                        basis, windowSize);
                System.err.println("USING woc");
            }

            Path wordListPath = DistributedCache.getLocalCacheFiles(conf)[0];
            BufferedReader br = new BufferedReader(
                    new FileReader(wordListPath.toString()));

            wordList = Sets.newHashSet();
            for (String line = null; (line = br.readLine()) != null; )
                wordList.add(line);
            System.out.printf("Num valid words: %d\n", wordList.size());
        }

        /**
         * {@inheritDoc}
         */
        public void map(ImmutableBytesWritable key,
                        Result row, 
                        Context context)
                throws IOException, InterruptedException {
            List<DependencyTreeNode> nodes = Lists.newArrayList();
            for (Sentence sentence : table.sentences(row))
                for (DependencyTreeNode node : sentence.dependencyParseTree())
                    nodes.add(node);
            DependencyTreeNode[] tree = nodes.toArray(new DependencyTreeNode[0]);

            // Iterate over each tree node and count all valid co-occurring
            // terms connected by a valid dependency path.
            for (int i = 0; i < tree.length; ++i) {
                if (!wordList.contains(tree[i].word()))
                    continue;
                SparseDoubleVector vector = generator.generateContext(tree, i);
                emitContext(tree[i].word(), basis, vector, context);
                context.getCounter("WordsiMR", "Contexts").increment(1);
            }
        }
    }

    public static <T> void emitContext(String focus, 
                                       BasisMapping<T, String> basis,
                                       SparseDoubleVector vector,
                                       Mapper.Context context)
                throws IOException, InterruptedException {
        StringBuilder builder = new StringBuilder();
        for (int c : vector.getNonZeroIndices()) {
            String term = basis.getDimensionDescription(c);
            double score = vector.get(c);
            builder.append(score).append(",");
            builder.append(term).append("|");
        }
        context.write(new Text(focus), new Text(builder.toString()));
    }
}

