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
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.filter.Filter;

import java.io.IOException;


/**
 * This sub class of {@link EvidenceInstanceBuilder} creates {@link Instance}s
 * that have a class label of {@link HypernymStatus#KNOWN_HYPERNYM} for all rows
 * that are marked as either {@link HypernymStatus#NOVEL_HYPERNYM} or {@link
 * HypernymStatus#NOVEL_HYPONYM}.
 *
 * @author Keith Stevens
 */
public class HypernymLearningInstanceBuilder extends EvidenceInstanceBuilder {

  /**
   * Builds a new instance of a {@link HypernymLearningInstanceBuilder}.
   *
   * @param attributeMap The {@link AttributeMap} that specifies the index
   *        for each feature label.
   * @param useBinaryForPathFeatures If true, dependency path counts will be
   *        turned into binary values, i.e., true for any non zero count.
   */
  public HypernymLearningInstanceBuilder(AttributeMap attributeMap,
                                         boolean useBinaryForPathFeatures,
                                         String source)
      throws IOException {
      super(attributeMap, useBinaryForPathFeatures, source);
  }

  /**
   * Returns the class qualifier for the {@link
   * WordNetEvidenceSchema#HYPERNYM_EVIDENCE} column.
   */
  protected byte[] getClassQualifier() {
    return WordNetEvidenceSchema.HYPERNYM_EVIDENCE.getBytes();
  }

  /**
   * Returns 1 for all rows that have class labels  {@link
   * HypernymStatus#NOVEL_HYPERNYM} or {@link HypernymStatus#NOVEL_HYPONYM} and
   * -1 for all others.  The return values are compatible with those returned
   *  from {@link HypernymTrainingInstanceBuilder}.
   */
  protected String getClassValue(Result resultRow, byte[] classQualifier) {
    byte[] bytes = resultRow.getValue(
        WordNetEvidenceSchema.CLASS_CF.getBytes(), classQualifier);
    if (bytes == null)
      return "";

    HypernymStatus status = HypernymStatus.valueOf(new String(bytes));
    if (status == HypernymStatus.NOVEL_HYPONYM)
      return HypernymStatus.KNOWN_HYPERNYM.toString();
    return "";
  }

  /**
   * Adds a filter such that only rows with a class value of {@link
   * HypernymStatus#NOVEL_HYPERNYM} are returned.
   */
  protected void setFilter(Scan scan) {
    scan.setFilter(new SingleColumnValueFilter(
          WordNetEvidenceSchema.CLASS_CF.getBytes(), getClassQualifier(),
          CompareOp.EQUAL,
          HypernymStatus.NOVEL_HYPERNYM.toString().getBytes()));
  }
}
