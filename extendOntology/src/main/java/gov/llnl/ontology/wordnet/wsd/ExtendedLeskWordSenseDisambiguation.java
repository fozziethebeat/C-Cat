/*
 * Copyright (c) 2011, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
 *
 * This file is part of the C-Cat package and is covered under the terms and
 * conditions therein.
 *
 * The S-Space package is free software: you can redistribute it and/or modify
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

package gov.llnl.ontology.wordnet.wsd;

import gov.llnl.ontology.wordnet.ExtendedLeskSimilarity;
import gov.llnl.ontology.wordnet.OntologyReader;


/**
 * A {@link WordSenseDisambiguation} implementation using the {@link
 * ExtendedLeskSimilarity} measure.
 *
 * </p>
 *
 * This class <b>is</b> thread safe.
 *
 * @see LeskWordSenseDisambiguation
 * @author Keith Stevens
 */
public class ExtendedLeskWordSenseDisambiguation 
        extends LeskWordSenseDisambiguation {

    /**
     * {@inheritDoc}
     */
    public void setup(OntologyReader reader) {
        this.reader = reader;
        this.sim = new ExtendedLeskSimilarity();
    }

    /**
     * Returns "eld", the acronyms for this {@link WordSenseDisambiguation}
     * algorithm.
     */
    public String toString() {
        return "eld";
    }
}
