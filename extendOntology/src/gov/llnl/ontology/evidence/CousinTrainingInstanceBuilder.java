/*
 * Copyright 2010 Keith Stevens 
 *
 * This file is part of the S-Space package and is covered under the terms and
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

package gov.llnl.ontology.evidence;

import gov.llnl.ontology.table.WordNetEvidenceSchema;

import gov.llnl.ontology.wordnet.SynsetRelations.HypernymStatus;

import org.apache.hadoop.hbase.client.Result;

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;


/**
 * This {@link EvidenceInstanceBuilder} composes training instances for a cousin
 * classifier.  For class labels, it uses strings of the format "m-n" where m
 * will equal to or greater than n and m,n will be less than a maximum specified
 * depth or equal to {@link Integer#MAX_VALUE}.  
 *
 * @author Keith Stevens
 */
public class CousinTrainingInstanceBuilder extends EvidenceInstanceBuilder {

  /**
   * Creates a new {@link CousinTrainingInstanceBuilder} from a given {@link
   * AttributeMap} and a {@code maxCousinDepth}.
   */
  public CousinTrainingInstanceBuilder(AttributeMap attributeMap,
                                       boolean ignored,
                                       String source) throws IOException {
      super(attributeMap, false, source);
  }

  /**
   * Returns {@link WordNetEvidenceSchema#COUSIN_EVIDENCE}.
   */
  protected byte[] getClassQualifier() {
    return WordNetEvidenceSchema.COUSIN_EVIDENCE.getBytes();
  }

  protected String getClassValue(Result resultRow, byte[] classQualifier) {
    byte[] bytes = resultRow.getValue(
        WordNetEvidenceSchema.CLASS_CF.getBytes(), classQualifier);
    return new String(bytes);
  }
}
