package gov.llnl.ontology.mapreduce.stats;

import gov.llnl.ontology.mapreduce.CorpusTableMR;
import gov.llnl.ontology.util.MRArgOptions;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.IOException;


/**
 * @author Keith Stevens
 */
public class ExtractKeysMR extends CorpusTableMR {

    /**
     * Runs the {@link ExtractKeysMR}.
     */
    public static void main(String[] args) throws Exception {
        ToolRunner.run(HBaseConfiguration.create(), new ExtractKeysMR(), args);
    }

    /**
     * {@inheritDoc}
     */
    protected Class mapperClass() {
        return ExtractKeysMapper.class;
    }

    /**
     * Returns the {@link Class} object for the Mapper Value of this task.
     */
    protected Class mapperKeyClass() {
        return ImmutableBytesWritable.class;
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
        job.setCombinerClass(Reducer.class);
        job.setReducerClass(Reducer.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        TextOutputFormat.setOutputPath(
                job, new Path(options.getPositionalArg(0)));
        job.setNumReduceTasks(24);
    }

    public static class ExtractKeysMapper
            extends TableMapper<ImmutableBytesWritable, IntWritable> {

        private static final IntWritable ONE = new IntWritable(1);

        /**
         * {@inheritDoc}
         */
        public void map(ImmutableBytesWritable key,
                        Result row, 
                        Context context)
                throws IOException, InterruptedException {
            context.write(key, ONE);
        }
    }
}

