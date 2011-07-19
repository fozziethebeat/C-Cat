package gov.llnl.ontology.mapreduce.stats;

import gov.llnl.ontology.mapreduce.EvidenceTableMR;
import gov.llnl.ontology.mapreduce.table.EvidenceTable;
import gov.llnl.ontology.text.Sentence;
import gov.llnl.ontology.util.Counter;
import gov.llnl.ontology.util.MRArgOptions;
import gov.llnl.ontology.util.StringPair;

import edu.ucla.sspace.util.ReflectionUtil;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.lib.reduce.IntSumReducer;

import java.io.IOException;
import java.util.Map;


/**
 * @author Keith Stevens
 */
public class DependencyPathCountMR extends EvidenceTableMR {

    /**
     * Runs the {@link DependencyPathCountMR}.
     */
    public static void main(String[] args) throws Exception {
        ToolRunner.run(HBaseConfiguration.create(),
                       new DependencyPathCountMR(), args);
    }

    /**
     * {@inheritDoc}
     */
    protected Class mapperClass() {
        return DependencyPathCountMapper.class;
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

    public static class DependencyPathCountMapper
            extends TableMapper<Text, IntWritable> {

        private EvidenceTable table;

        /**
         * {@inheritDoc}
         */
        public void setup(Context context)
                throws IOException, InterruptedException {
            Configuration conf = context.getConfiguration();
            table = ReflectionUtil.getObjectInstance(conf.get(EVIDENCE));
            table.table();
        }

        /**
         * {@inheritDoc}
         */
        public void map(ImmutableBytesWritable key,
                        Result row, 
                        Context context)
                throws IOException, InterruptedException {
            Counter<String> counts = table.getDependencyPaths(row);
            for (Map.Entry<String, Integer> count : counts)
                context.write(new Text(count.getKey()),
                              new IntWritable(count.getValue()));
        }
    }
}
