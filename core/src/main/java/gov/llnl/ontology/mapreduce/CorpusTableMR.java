package gov.llnl.ontology.mapreduce;

import gov.llnl.ontology.mapreduce.table.CorpusTable;

import com.google.common.collect.Sets;

import edu.ucla.sspace.util.ReflectionUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.IdentityTableReducer;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.util.Tool;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;


/**
 * @author Keith Stevens
 */
public abstract class CorpusTableMR extends Configured implements Tool {

    /**
     * The configuration key prefix.
     */
    public static String CONF_PREFIX =
        "gov.llnl.ontology.mapreduce.CorpusTableMR";

    /**
     * The configuration key for setting the {@link CorpusTable}.
     */
    public static String TABLE = CONF_PREFIX + ".corpusTable";

    /**
     * Acquire the logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(CorpusTableMR.class);

    /**
     * Add more command line arguments.  By default, this adds no options.
     */
    protected void addOptions(MRArgOptions options) {
    }

    /**
     * Returns a descriptive job name for this map reduce task.
     */
    protected String jobName() {
        return "Ingest Corpus";
    }

    /**
     * Returns true if the {@link MRArgOptions} contains a valid value for each
     * requried option.  By default, this does no validation.
     */
    protected void validateOptions(MRArgOptions options) {
    }

    /**
     * Copies command line arguments to a {@link Configuration} so that
     * Map/Reduce jobs can utilize the values set.  By default, this does no
     * configuration.
     */
    protected void setupConfiguration(MRArgOptions options,
                                      Configuration conf) {
    }

    /**
     * Sets up the Reducer for this job.  By default, it is a {@link
     * IdentityTableReducer}.
     */
    protected void setupReducer(String tableName,
                                Job job,
                                MRArgOptions options) throws IOException {
        // Setup an empty reducer.
        TableMapReduceUtil.initTableReducerJob(tableName,
                                               IdentityTableReducer.class, 
                                               job);
        job.setNumReduceTasks(0);
    }

    protected void addToDistrubutedCache(String fileName, Configuration conf) {
        try {
            DistributedCache.addCacheFile(new URI(fileName), conf);
        } catch (URISyntaxException use) {
            use.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Returns the {@link Class} object for the Mapper task.
     */
    protected abstract Class mapperClass();

    /**
     * Returns the {@link Class} object for the Mapper Key of this task.  By
     * default this returns {@link ImmutableBytesWritable}.
     */
    protected Class mapperKeyClass() {
        return ImmutableBytesWritable.class;
    }

    /**
     * Returns the {@link Class} object for the Mapper Value of this task.  By
     * default, this returns {@link Put}.
     */
    protected Class mapperValueClass() {
        return Put.class;
    }

    /**
     * {@inheritDoc}
     */
    public int run(String[] args)
            throws IOException, InterruptedException, ClassNotFoundException {
        // Setup the main arguments used.
        MRArgOptions options = new MRArgOptions();
        addOptions(options);

        // Parse and validate the arguments.
        LOG.info("Parse Options");
        options.parseOptions(args);
        validateOptions(options);

        // Setup the configuration.
        LOG.info("Setup Configuration");
        Configuration conf = getConf();
        conf.set(TABLE, options.corpusTableType());
        setupConfiguration(options, conf);

        // Create the corpus table and setup the scan.
        LOG.info("Setup Table Scan");
        CorpusTable table = options.corpusTable();
        Scan scan = new Scan();
        table.setupScan(scan, options.sourceCorpus());

        // Create the job and set the jar.
        LOG.info("Setup Job");
        Job job = new Job(conf, jobName());
        job.setJarByClass(CorpusTableMR.class);

        // Setup the mapper class.
        TableMapReduceUtil.initTableMapperJob(table.tableName(),
                                              scan, 
                                              mapperClass(),
                                              mapperKeyClass(),
                                              mapperValueClass(),
                                              job);
        setupReducer(table.tableName(), job, options);

        LOG.info("Start Mapper job"); 
        // Run the job.
        job.waitForCompletion(true);
        LOG.info("Job Completed");

        return 0;
    }

    /**
     * A simple base class for any {@link CorpusTableMR} job.  Most
     * implementations need only implement {@link #map(ImmutableBytesWritable,
     * Result, Context)}.  Those that need more than just a {@link CorpusTable}
     * should override {@link #setup(Context, Configuration)} to create any
     * additional data structures or data sources.
     */
    public static abstract class CorpusTableMapper<K, V>
                extends TableMapper<K, V> {
                
        /**
         * The {@link CorpusTable} responsible for reading row data.
         */
        protected CorpusTable table;

        /**
         * Initializes the {@link CorpusTable} for this {@link
         * CorpusTableMapper} and calls {@link #setup(Context, Configuration)}.
         */
        public void setup(Context context)
                throws IOException, InterruptedException {
            context.setStatus("Setup");
            Configuration conf = context.getConfiguration();
            table = ReflectionUtil.getObjectInstance(conf.get(TABLE));
            table.table();
            context.setStatus("CorpusTable created");

            setup(context, conf);
        }

        public Set<String> loadWordList(Configuration conf) {
            try {
                Path wordListPath = 
                    DistributedCache.getLocalCacheFiles(conf)[0];
                BufferedReader br = new BufferedReader(
                        new FileReader(wordListPath.toString()));

                Set<String> wordList = Sets.newHashSet();
                for (String line = null; (line = br.readLine()) != null; )
                    wordList.add(line.trim());
                return wordList;
            } catch (IOException ioe) {
                throw new IOError(ioe);
            }
        }

        /**
         * Sets up any addition data classes or information needed by the {@link
         * CorpusTableMapper}.  By default, this does nothing.
         */
        protected void setup(Context context, Configuration conf)
            throws IOException, InterruptedException {
        }
    }
}

