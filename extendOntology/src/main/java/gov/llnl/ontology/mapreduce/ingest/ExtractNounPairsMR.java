package gov.llnl.ontology.mapreduce.ingest;

import gov.llnl.ontology.mapreduce.CorpusTableMR;
import gov.llnl.ontology.mapreduce.table.CorpusTable;
import gov.llnl.ontology.mapreduce.table.EvidenceTable;
import gov.llnl.ontology.text.Sentence;
import gov.llnl.ontology.util.Counter;
import gov.llnl.ontology.util.MRArgOptions;

import edu.ucla.sspace.dependency.DependencyRelation;
import edu.ucla.sspace.dependency.DependencyPath;
import edu.ucla.sspace.dependency.DependencyPathAcceptor;
import edu.ucla.sspace.dependency.DependencyTreeNode;
import edu.ucla.sspace.dependency.FilteredDependencyIterator;
import edu.ucla.sspace.util.ReflectionUtil;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.util.Iterator;


/**
 * @author Keith Stevens
 */
public class ExtractNounPairsMR extends CorpusTableMR {

    /**
     * The configuration key for the {@link DependencyPathAcceptor}.
     */
    public static final String ACCEPTOR = CONF_PREFIX + ".acceptor";

    /**
     * The configuration key for the maximum valid depenendency path length.
     */
    public static final String PATH_LENGTH = CONF_PREFIX + ".pathLength";

    /**
     * The configuration key for the {@link EvidenceTable}.
     */
    public static final String EVIDENCE = CONF_PREFIX + ".evidence";

    /**
     * Runs the {@link ExtractNounPairsMR}.
     */
    public static void main(String[] args) throws Exception {
        ToolRunner.run(HBaseConfiguration.create(),
                       new ExtractNounPairsMR(), args);
    }
    /**
     * {@inheritDoc}
     */
    protected Class mapperClass() {
        return ExtractNounPairsMapper.class;
    }

    /**
     * {@inheritDoc}
     */
    protected Class mapperKeyClass() {
        return Text.class;
    }

    /**
     * {@inheritDoc}
     */
    protected Class mapperValueClass() {
        return Text.class;
    }

    protected void setupReducer(String tableName,
                                Job job,
                                MRArgOptions options) throws IOException {
        EvidenceTable evidence = options.evidenceTable();
        TableMapReduceUtil.initTableReducerJob(evidence.tableName(),
                                               ExtractNounPairReducer.class,
                                               job);
        job.setNumReduceTasks(24);
    }

    /**
     * {@inheritDoc}
     */
    protected void addOptions(MRArgOptions options) {
        options.addOption('a', "pathAcceptor",
                          "Specifies the DependencyPathAcceptor to use for " +
                          "filtering out valid dependency paths",
                          true, "CLASSNAME", "Optional");
        options.addOption('l', "pathLength",
                          "Specifies the longest valid dependency path. " +
                          "(Default: 3)",
                          true, "INT", "Optional");
    }

    /**
     * {@inheritDoc}
     */
    protected void setupConfiguration(MRArgOptions options, 
                                      Configuration conf) {
        conf.set(ACCEPTOR, options.getStringOption('a'));
        conf.set(PATH_LENGTH, options.getStringOption('l'));
    }

    public static class ExtractNounPairsMapper 
            extends TableMapper<Text, Text> {

        /**
         * The {@link CorpusTable} that dictates the structure of the table
         * containing a corpus.
         */
        private CorpusTable table;

        private DependencyPathAcceptor acceptor;

        private int maxLength;

        /**
         * {@inheritDoc}
         */
        public void setup(Context context) {
            Configuration conf = context.getConfiguration();
            table = ReflectionUtil.getObjectInstance(conf.get(TABLE));
            table.table();
            acceptor = ReflectionUtil.getObjectInstance(conf.get(ACCEPTOR));
            maxLength = Integer.parseInt(conf.get(PATH_LENGTH));
        }


        /**
         * {@inheritDoc}
         */
        public void map(ImmutableBytesWritable key,
                        Result row, 
                        Context context)
                throws IOException, InterruptedException{
            String source = table.sourceCorpus(row);
            for (Sentence sentence : table.sentences(row)) {
                for (DependencyTreeNode node : sentence.dependencyParseTree()) {
                    if (!node.pos().toLowerCase().startsWith("n"))
                        continue;
                    String first = node.word();

                    Iterator<DependencyPath> paths = 
                        new FilteredDependencyIterator(
                                node, acceptor, maxLength);
                    while (paths.hasNext()) {
                        DependencyPath path = paths.next();
                        if (!path.last().pos().toLowerCase().startsWith("n"))
                            continue;
                        String second = path.last().word();
                        emitPath(first, second, source, path, context);
                    }
                }

            }
        }

        private void emitPath(String first, String second, String source,
                              DependencyPath path, Context context) 
                throws IOException, InterruptedException {
            StringBuilder builder = new StringBuilder();
            for (DependencyRelation relation : path)
                builder.append(relation.relation()).append(":");
            String key = first + ":" + second + ":" + source;
            context.write(new Text(key), new Text(builder.toString()));
        }
    }

    public static class ExtractNounPairReducer
            extends TableReducer<Text, Text, Put> {

        private EvidenceTable evidence;

        /**
         * {@inheritDoc}
         */
        protected void setup(Context context) {
            Configuration conf = context.getConfiguration();
            evidence = ReflectionUtil.getObjectInstance(conf.get(EVIDENCE));
            evidence.table();
        }

        /**
         * {@inheritDoc}
         */
        protected void reduce(Text key,
                              Iterable<Text> values,
                              Context context)
                throws IOException, InterruptedException {
            String[] wordPairSource = key.toString().split(":");
            if (wordPairSource.length != 3)
                return;

            Counter<String> pathCount = new Counter<String>();
            for (Text path : values)
                pathCount.count(path.toString());

            evidence.putDependencyPaths(wordPairSource[0], wordPairSource[1],
                                     wordPairSource[2], pathCount);
        }

        /**
         * {@inheritDoc}
         */
        protected void cleanup(Context context) {
            evidence.close();
        }
    }
}

