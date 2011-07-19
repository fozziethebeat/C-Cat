package gov.llnl.ontology.mapreduce.stats;

import gov.llnl.ontology.mapreduce.CorpusTableMR;
import gov.llnl.ontology.mapreduce.table.CorpusTable;
import gov.llnl.ontology.text.Sentence;
import gov.llnl.ontology.util.MRArgOptions;
import gov.llnl.ontology.util.StringPair;

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
import java.util.Set;


/**
 * @author Keith Stevens
 */
public class TagWordStatsMR extends CorpusTableMR {

    public static void main(String[] args) throws Exception {
        ToolRunner.run(HBaseConfiguration.create(), new TagWordStatsMR(), args);
    }

    public Class mapperClass() {
        return TagWordStatsMapper.class;
    }

    public Class mapperKeyClass() {
        return Text.class;
    }

    public Class mapperValueClass() {
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

    public static class TagWordStatsMapper
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
            Set<String> categories = table.getCategories(row);
            for (Sentence sentence : table.sentences(row))
                for (StringPair word : sentence.taggedTokens())
                    for (String category : categories)
                        context.write(new Text(category), new Text(word.x));
        }
    }
}
