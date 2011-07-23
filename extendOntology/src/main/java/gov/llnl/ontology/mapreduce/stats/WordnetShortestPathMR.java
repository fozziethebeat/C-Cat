package gov.llnl.ontology.mapreduce.stats;

import gov.llnl.ontology.mapreduce.table.CorpusTable;
import gov.llnl.ontology.text.hbase.LineDocInputFormat;
import gov.llnl.ontology.wordnet.OntologyReader;
import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.SynsetRelations;
import gov.llnl.ontology.wordnet.WordNetCorpusReader;

import edu.ucla.sspace.common.ArgOptions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;


/**
 * @author Keith Stevens
 */
public class WordnetShortestPathMR extends Configured implements Tool {

    public static final String CONF_BASE =
        "gov.llnl.ontology.mapreduce.stats.WordnetShortestPathMR";

    public static final String WORDNET =
        CONF_BASE + ".wordnet";

    public static final String URI_BASE = "maprfs:///";

    public static final String TEMP_TERM_PAIR_PATH = "wordnet-pair-file";

    /**
     * Runs the {@link IngestCorpusMR}.
     */
    public static void main(String[] args) throws Exception {
        ToolRunner.run(HBaseConfiguration.create(),
                       new WordnetShortestPathMR(), args);
    }

    public int run(String[] args) throws Exception {
        ArgOptions options = new ArgOptions();
        options.addOption('w', "wordnetDir",
                          "The directory path to the wordnet data files",
                          true, "PATH", "Required");

        options.parseOptions(args);
        if (!options.hasOption('w')) {
            System.err.println(
                    "usage: java WordnetShortestPathMR [OPTIONS] <outdir>\n"+
                    options.prettyPrint());
        }

        // Open the wordnet reader and gather the set of all Synsets known by
        // the ontology.
        OntologyReader reader = WordNetCorpusReader.initialize(
                options.getStringOption('w'));
        Set<Synset> synsetSet = new HashSet<Synset>();
        for (String lemma : reader.wordnetTerms())
            for (Synset synset : reader.getSynsets(lemma))
                synsetSet.add(synset);

        // Compute each pairing of Synsets and write that pairing to a file in
        // HDFS.
        Synset[] synsets = synsetSet.toArray(new Synset[0]);
        PrintStream outStream = createPrintStream();
        for (int i = 0; i < synsets.length; ++i)
            for (int j = i+1; j < synsets.length; ++j)
                outStream.printf("%s|%s\n",
                                 synsets[i].getName(), synsets[j].getName());
        outStream.close();

        // Store the wordnet directory information so that the mappers can load
        // it up.  They need it to figure out the shortest path information.
        Configuration conf = getConf();
        conf.set(WORDNET, options.getStringOption('w'));

        // Setup the job information.
        Job job = new Job(conf, "Compute Wordnet Shortest Paths");
        job.setJarByClass(WordnetShortestPathMR.class);

        job.setMapperClass(WordnetShortestPathMapper.class);

        // The input file will be the temporary file created with the synset
        // pairings.
        job.setInputFormatClass(LineDocInputFormat.class);
        FileInputFormat.addInputPath(job, new Path(TEMP_TERM_PAIR_PATH));

        // The mappers do all of the real work, so we just write their output
        // straight to disk.
        job.setCombinerClass(Reducer.class);
        job.setReducerClass(Reducer.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        TextOutputFormat.setOutputPath(
                job, new Path(options.getPositionalArg(0)));

        // Start the job.
        job.waitForCompletion(true);

        return 0;
    }

    /**
     * Creates a new temporary file on HDFS that will store the {@link Synset}
     * name pairs to be used for finding the shortest distance between all
     * nodes.
     */
    private static PrintStream createPrintStream() throws Exception {
        Configuration conf = new Configuration();
        FileSystem fs = FileSystem.get(URI.create(URI_BASE), conf);
        Path finalPath = new Path(TEMP_TERM_PAIR_PATH);
        FSDataOutputStream ostr = fs.create(finalPath);
        return new PrintStream(ostr);
    }

    public static class WordnetShortestPathMapper
            extends Mapper<LongWritable, Text, Text, IntWritable> {

        private OntologyReader reader;

        public void setup(Context context)
                throws IOException, InterruptedException {
            Configuration conf = context.getConfiguration();
            reader = WordNetCorpusReader.initialize(conf.get(WORDNET));
        }

        public void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException {
            String[] terms = value.toString().split("\\|");
            Synset synset1 = reader.getSynset(terms[0]);
            Synset synset2 = reader.getSynset(terms[1]);
            int shortestPath = SynsetRelations.shortestPathDistance(
                    synset1, synset2);
            context.write(value, new IntWritable(shortestPath));
        }
    }
}
