/*
 * Copyright (c) 2010, Lawrence Livermore National Security, LLC. Produced at
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

package gov.llnl.ontology.evidence;

import gov.llnl.ontology.table.WordNetEvidenceSchema;

import gov.llnl.ontology.wordnet.SynsetRelations.HypernymStatus;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;

import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;

import java.io.IOException;


/**
 * This {@EvidenceInstanceBuilder} creates training instances from a {@link
 * Result} row in the {@link WordNetEvidenceSchema} table.  The possible class
 * values are {@link HypernymStatus#KNOWN_HYPERNYM} or {@link
 * HypernymStatus#KNOWN_NON_HYPERNYM}. 
 *
 * @author Keith Stevens
 */
public class HypernymTrainingInstanceBuilder extends EvidenceInstanceBuilder {

  /**
   * Creates a new {@link HypernymTrainingInstanceBuilder}.
   *
   * @param attributeMap The {@link AttributeMap} that is responsible for
   *        defining the feature space.
   * @param useBinaryForPathFeatures If true, each feature will have a value of
   *        1 if there is a non zero value.
   */
  public HypernymTrainingInstanceBuilder(AttributeMap attributeMap,
                                         boolean useBinaryForPathFeatures,
                                         String source) throws IOException {
      super(attributeMap, useBinaryForPathFeatures, source);
  }

  /**
   * {@inheritDoc}
   */
  protected byte[] getClassQualifier() {
    return WordNetEvidenceSchema.HYPERNYM_EVIDENCE.getBytes();
  }

  /**
   * {@inheritDoc}
   */
  protected String getClassValue(Result resultRow, byte[] classQualifier) {
    byte[] bytes = resultRow.getValue(
        WordNetEvidenceSchema.CLASS_CF.getBytes(), classQualifier);
    if (bytes == null)
      return "";

    String statStr = new String(bytes);
    HypernymStatus status = HypernymStatus.valueOf(statStr);
    if (status == HypernymStatus.KNOWN_HYPERNYM || 
        status == HypernymStatus.KNOWN_NON_HYPERNYM)
      return statStr;
    return "";
  }

  /**
   * Adds a filter such that only rows with a class value of {@link
   * HypernymStatus#NOVEL_HYPERNYM} are returned.
   */
  protected void setFilter(Scan scan) {
    FilterList filters = new FilterList(FilterList.Operator.MUST_PASS_ONE);
    filters.addFilter(new SingleColumnValueFilter(
          WordNetEvidenceSchema.CLASS_CF.getBytes(), getClassQualifier(),
          CompareOp.EQUAL,
          HypernymStatus.KNOWN_HYPERNYM.toString().getBytes()));
    filters.addFilter(new SingleColumnValueFilter(
          WordNetEvidenceSchema.CLASS_CF.getBytes(), getClassQualifier(),
          CompareOp.EQUAL,
          HypernymStatus.KNOWN_NON_HYPERNYM.toString().getBytes()));
    scan.setFilter(filters);
  }
}
