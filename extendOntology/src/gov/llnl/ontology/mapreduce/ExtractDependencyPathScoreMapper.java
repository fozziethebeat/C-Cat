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

package gov.llnl.ontology.mapreduce;

import edu.ucla.sspace.dependency.DependencyPath;
import edu.ucla.sspace.dependency.DependencyPathWeight;
import edu.ucla.sspace.dependency.LengthPathWeight;

import org.apache.hadoop.io.Text;

import java.io.IOException;


/**
 * A mapper that scores dependency paths and emites their score, along with the
 * final word in the path.  This is useful for building semantic spaces from
 * dependency path information.
 *
 * @author Keith Stevens
 */
public class ExtractDependencyPathScoreMapper
    extends ExtractDependencyPathMapper {

  /**
   * The {@link DependencyPathWeight} responsible for scoring each path.
   */
  private DependencyPathWeight weight; 

  /**
   * {@inheritDoc}.
   */
  public void setup(Context context) {
    super.setup(context);
    weight = new LengthPathWeight();
  }

  /**
   * Emits {@code childTerm} as the key and "{@code parentTerm}|score({@code
   * path})" as the value.
   *
   * @param childTerm The first term in the dependency path.  This serves as the
   *        emitted key.
   * @param parentTerm The final term in the dependency path.  This serves as
   *        part of the emitted value.
   * @param source The document collection name that generated this data value.
   * @param path The {@link DependencyPath}.
   * @param context The {@link Context} used to emit a key, value pair.
   *
   * @throws IOException
   * @throws InterruptedException
   */
  protected void emitPath(String childTerm, String parentTerm, String source,
                          DependencyPath path, Context context) 
      throws IOException, InterruptedException {
    String occurrence = parentTerm + "|" + weight.scorePath(path);
    context.write(new Text(childTerm), new Text(occurrence));
    context.getCounter("count features", "noun pair").increment(1);
  }
}
