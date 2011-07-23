/*
 * Copyright (c) 2011, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
 *
 * This file is part of the C-Cat package and is covered under the terms and
 * conditions therein.
 *
 * The C-Cat package is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation and distributed hereunder to you.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND NO REPRESENTATIONS OR WARRANTIES,
 * EXPRESS OR IMPLIED ARE MADE.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, WE MAKE
 * NO REPRESENTATIONS OR WARRANTIES OF MERCHANT- ABILITY OR FITNESS FOR ANY
 * PARTICULAR PURPOSE OR THAT THE USE OF THE LICENSED SOFTWARE OR DOCUMENTATION
 * WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER
 * RIGHTS.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

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

    /**
     * Validates the parsed options.  If any of the required options do not
     * exist, an error {@code message} will be printed instructing the user with
     * the available options and the program will immediately exit.  When
     * exiting, the format output format will be:
     *
     * usage: java CLASSNAME [OPTIONS] extraAgs
     * </br>
     * Options:
     * </br>
     * . . .
     * </br>
     * {@code message}
     * </br>
     *
     * @param message An error message to print that is specific to the calling
     *        program
     * @param extraOptionDescription A one line summary of the extra positional
     *        options that are expcted
     * @param c The class type of the running program
     * @param expectedPosArgs The number of expected positional arguments
     * @param requiredList A list of chars denoting the required options
     */
    public void validate(String message, String extraOptionDescription,
                         Class c, int expectedPosArgs, char ...requiredList) {
        for (char required : requiredList)
            if (!hasOption(required))
                fail(message, extraOptionDescription, c);
        if (expectedPosArgs >= 0)
            if (numPositionalArgs() != expectedPosArgs)
                fail(message, extraOptionDescription, c);
    }

    /**
     * Reports an usage message to the user and exits with failure.  The usage
     * message will be in the following format.
     *
     * usage: java CLASSNAME [OPTIONS] extraAgs
     * </br>
     * Options:
     * </br>
     * . . .
     * </br>
     * {@code message}
     * </br>
     *
     * @param message An error message to print that is specific to the calling
     *        program
     * @param extraOptionDescription A one line summary of the extra positional
     *        options that are expcted
     * @param c The class type of the running program
     */
    public void fail(String message, String extraOptionDescription, Class c) {
        System.out.printf("usage: java %s [OPTIONS] %s\n%s\n%s\n",
                          c.getName(), message,
                          extraOptionDescription, prettyPrint());
        System.exit(1);
    }
}
