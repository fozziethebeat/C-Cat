package gov.llnl.ontology.text.hbase;

import org.apache.hadoop.conf.Configuration;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import org.apache.hadoop.hbase.io.ImmutableBytesWritable;

import org.apache.hadoop.io.Text;

import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


/**
 * A {@link FileInputFormat} for xml files that are gzipped.  Before starting a
 * job, call {@link #setXMLTags} to specify the text of the document
 * deliminiting tags.
 *
 * @author Keith Stevens
 */
public class GzipXmlInputFormat
        extends FileInputFormat<ImmutableBytesWritable, Text> {

    public static void setXMLTags(Job job, String delimiter) {
        Configuration conf = job.getConfiguration();
        conf.set(XMLRecordReader.DELIMITER_TAG, delimiter);
    }

    /**
     * Returns a {@link GzipXmlRecordReader}.  The record reader will return
     * each tarred file.
     */
    public RecordReader createRecordReader(InputSplit split,
                                           TaskAttemptContext context)
            throws IOException, InterruptedException {
        return new XMLRecordReader(true);
    }

    /**
     * Returns a {@link List} of {@link FileSplit}s.  Each {@link FileSplit}
     * will be a gzipped tarball of xml documents.  Each tarred file should
     * contain a single document.
     */
    public List<InputSplit> getSplits(JobContext context) throws IOException {
        List<InputSplit> splits = new ArrayList<InputSplit>();

        // Get the list of zipped files to be processed and add each zipped file
        // as an InputSplit.
        FileSystem fs = FileSystem.get(context.getConfiguration());
        for (Path file : getInputPaths(context)) {
            // Check that the list of files exists.  Throw an exception if it
            // does not.
            if (fs.isDirectory(file) || !fs.exists(file))
                throw new IOException("File does not exist: " + file);

            // Read the contents of the file list and add each line as a
            // FileSplit.
            BufferedReader br = new BufferedReader(new InputStreamReader(
                        fs.open(file)));
            for (String line = null; (line = br.readLine()) != null; )
                splits.add(new FileSplit(
                            new Path(line), 0, Integer.MAX_VALUE, null));
        }
        return splits;
    }
}
