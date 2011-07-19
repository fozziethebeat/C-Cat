package gov.llnl.ontology.mapreduce.stats;

import gov.llnl.ontology.mapreduce.CorpusTableMR;
import gov.llnl.ontology.mapreduce.table.CorpusTable;
import gov.llnl.ontology.util.MRArgOptions;

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


/**
 * This MapReduce extracts the edges for a Tag-Tag network for documents within
 * a specific corpus.
 *
 * @author Keith Stevens
 */
public class TagNetworkMR extends CorpusTableMR {

    /**
     * Runs the {@link TokenCountMR}.
     */
    public static void main(String[] args) throws Exception {
        ToolRunner.run(HBaseConfiguration.create(), new TagNetworkMR(), args);
    }

    /**
     * {@inheritDoc}
     */
    protected Class mapperClass() {
        return TagNetworkMapper.class;
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
        job.setCombinerClass(WordSumReducer.class);
        job.setReducerClass(WordSumReducer.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        TextOutputFormat.setOutputPath(
                job, new Path(options.getPositionalArg(0)));
        job.setNumReduceTasks(24);
    }

    public static class TagNetworkMapper 
            extends TableMapper<Text, Text> {

        private CorpusTable table;

        /**
         * {@inheritDoc}
         */
        public void setup(Context context)
                throws IOException, InterruptedException {
            Configuration conf = context.getConfiguration();
            table = ReflectionUtil.getObjectInstance(conf.get(TABLE));
            table.table();
        }

        /**
         * {@inheritDoc}
         */
        public void map(ImmutableBytesWritable key,
                        Result row, 
                        Context context)
                throws IOException, InterruptedException {
            String[] categories = table.getCategories(row).toArray(
                    new String[0]);
            for (int i = 0; i < categories.length; ++i)
                for (int j = i+1; j < categories.length; ++j)
                    context.write(new Text(categories[i]),
                                  new Text(categories[j]));
        }
    }
}
