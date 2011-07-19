package gov.llnl.ontology.text.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


/**
 * @author Keith Stevens
 */
public class LineDocInputFormat 
        extends FileInputFormat<LongWritable, Text> {

    public RecordReader createRecordReader(InputSplit split,
                                           TaskAttemptContext context)
            throws IOException, InterruptedException {
        return new LineDocReader();
    }

    public class LineDocReader 
            extends RecordReader<LongWritable, Text> {

        private long position;

        private Text value;

        private BufferedReader br;

        public void initialize(InputSplit isplit, TaskAttemptContext context) 
                throws IOException, InterruptedException {
            position = 0;

            FileSplit split = (FileSplit) isplit;
            Path file = split.getPath();
            Configuration config = context.getConfiguration();
            FileSystem fs = file.getFileSystem(config);
            br = new BufferedReader(new InputStreamReader(
                        fs.open(split.getPath())));
        }

        public boolean nextKeyValue() throws IOException {
            position++;
            String line = br.readLine();
            if (line == null)
                return false;
            value = new Text(line);
            return true;
        }

        public LongWritable getCurrentKey() {
            return new LongWritable(position);
        }

        public Text getCurrentValue() {
            return value;
        }

        public float getProgress() throws IOException, InterruptedException {
            return .5f;
        }

        public void close() throws IOException {
            br.close();
        }
    }
}

