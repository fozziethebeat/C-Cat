
package gov.llnl.ontology.util;

import gov.llnl.ontology.mapreduce.table.CorpusTable;
import gov.llnl.ontology.mapreduce.table.EvidenceTable;

import edu.ucla.sspace.common.ArgOptions;

import edu.ucla.sspace.util.ReflectionUtil;


/**
 * This class sets up some basic options that will be used in a wide number of
 * map reduce jobs that connect to either a {@link CorpusTable} or a {@link
 * EvidenceTable}.
 *
 * </p>
 *
 * {@link MRArgOptions} provides the following options:
 *
 * <ul>
 *   <li>{@code -C}, {@code --corpusTable}: Specifies the type of {@link
 *   CorpusTable} being used.</li>
 *   <li>{@code -E}, {@code --EvidenceTable}: Specifies the type of {@link
 *   EvidenceTable} being used.</li>
 *   <li>{@code -S}, {@code --sourceCorpus}: Specifies name of the source corpus
 *   to be processed.</li>
 * </ul>
 *
 * @author Keith Stevens
 */
public class MRArgOptions extends ArgOptions {

    /**
     * Creates a new {@link MRArgOptions}.
     */
    public MRArgOptions() {
        addOption('C', "corpusTable", 
                  "Specifies the type of CorpusTable being used",
                  true, "CLASSNAME", "Optional");
        addOption('E', "evidenceTable",
                  "Specifies the type of EvidenceTable being used",
                  true, "CLASSNAME", "Optional");
        addOption('S', "sourceCorpus",
                  "Specifies the name of the source corpus to be processed.",
                  true, "CLASSNAME", "Optional");
    }

    /**
     * Returns an instance of the {@link CorpusTable} specified via the command
     * line.
     */
    public CorpusTable corpusTable() {
        return ReflectionUtil.getObjectInstance(getStringOption('C'));
    }

    /**
     * Returns the type of the {@link CorpusTable} specified via the command
     * line.
     */
    public String corpusTableType() {
        return getStringOption('C');
    }

    /**
     * Returns an instance of the {@link EvidenceTable} specified via the
     * command line.
     */
    public EvidenceTable evidenceTable() {
        return ReflectionUtil.getObjectInstance(getStringOption('E'));
    }

    /**
     * Returns the type of the {@link EvidenceTable} specified via the command
     * line.
     */
    public String evidenceTableType() {
        return getStringOption('E');
    }

    /**
     * Returns the name of the source corpus specified via the command line, or
     * the empty string if none was specified.
     */
    public String sourceCorpus() {
        return getStringOption('S', "");
    }
}
