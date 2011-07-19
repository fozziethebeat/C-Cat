package gov.llnl.ontology.mapreduce.stats;

import gov.llnl.ontology.util.Counter;

import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.IntWritable;

import java.io.IOException;
import java.util.Map;


/**
 * This {@link Reducer} counts the number of times that they {@code key} occures
 * with each of it's {@code value}s.  It emits each a tuple of the form: 
 *   (key,other_item), co-occurrence_count
 * Where the first item is emited as a single {@link Text} object and the count
 * is a {@link IntWritable} object.
 *
 * @author Keith Stevens
 */
public class WordSumReducer extends Reducer<Text, Text, Text, Text> {

    /**
     * {@inheritDoc}
     */
    public void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {
        Counter<String> occurrences = new Counter<String>();
        for (Text item : values)
            occurrences.count(item.toString());
        for (Map.Entry<String, Integer> e : occurrences)
            context.write(new Text(key.toString()),
                          new Text(e.getKey() + " " + e.getValue()));
    }
}
