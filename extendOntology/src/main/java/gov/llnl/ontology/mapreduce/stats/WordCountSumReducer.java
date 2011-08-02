package gov.llnl.ontology.mapreduce.stats;

import gov.llnl.ontology.util.Counter;

import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.IntWritable;

import java.io.IOException;
import java.util.Map;


/**
 * @author Keith Stevens
 */
public class WordCountSumReducer extends Reducer<Text, Text, Text, Text> {

    /**
     * Writes the co-occurrence counts as stored in a map of {@link Counter}s to
     * {@code context}.  This is simply a helper function that formats each
     * written co-occurrence in the format used by this {@link Reducer}.
     */
    public static void emitCounts(Map<String, Counter<String>> wocCounts,
                                  Mapper.Context context) 
            throws IOException, InterruptedException {
        for (Map.Entry<String, Counter<String>> e : wocCounts.entrySet()) {
            Text focusWord = new Text(e.getKey());
            for (Map.Entry<String, Integer> f : e.getValue()) {
                String otherWord = f.getKey();
                int count = f.getValue();
                Text out = new Text(otherWord + "|" + count);
                context.write(focusWord, out);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {
        Counter<String> occurrences = new Counter<String>();
        for (Text item : values) {
            String[] parts = item.toString().split("\\|");
            String term = parts[0];
            int count = Integer.parseInt(parts[1]);
            occurrences.count(term, count);
        }

        for (Map.Entry<String, Integer> e : occurrences)
            context.write(new Text(key.toString()),
                          new Text(e.getKey() + " " + e.getValue()));
    }
}
