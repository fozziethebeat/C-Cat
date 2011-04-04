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

import gov.llnl.ontology.hbase.DocumentReader;
import gov.llnl.ontology.hbase.TrinidadDocumentReader;

import trinidad.hbase.mapreduce.annotation.ReportProgressThread;

import edu.ucla.sspace.dependency.FilteredDependencyIterator;
import edu.ucla.sspace.dependency.DependencyPath;
import edu.ucla.sspace.dependency.DependencyRelation;
import edu.ucla.sspace.dependency.DependencyTreeNode;
import edu.ucla.sspace.dependency.UniversalPathAcceptor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import org.apache.hadoop.hbase.client.Result;

import org.apache.hadoop.hbase.io.ImmutableBytesWritable;

import org.apache.hadoop.hbase.mapreduce.TableMapper;

import org.apache.hadoop.io.Text;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 * This mapper reads each document in the BigTable and inspects each
 * dependency parsed sentence.  For each sentence, noun pairs are evaluated
 * and the dependency path connecting the two nouns is emitted for all pairs
 * found.  
 *
 * @author Keith Stevens
 */
public class ExtractDependencyPathMapper extends TableMapper<Text, Text> {

  /**
   * The separator used for separating two noun pairs from their source value.
   */
  public static final String SOURCE_SEPARATOR = ";;";

  /**
   * The separator used for separating two noun pairs that function as a key.
   */
  public static final String NOUN_SEPARATOR = "|";

  /**
   * The {@link DocumentReader} that is used to extract document text and
   * dependency parse trees from a separate HBase table.
   */
  private DocumentReader reader;

  private Set<String> wordList;

  /**
   * A backup parser for when parse tree information is not used.  This is
   * currently a huge hack.  Code using this should be refactored to not
   * access to {@link DocSchema} directly.
  private ParserUtil parserUtil;
   */

  /**
   * A pointer to the Trinidad based document table used for when dependency
   * parse trees are not available.  Again, part of the huge hack. 
  private HTable docTable;
   */

  /**
    * Initialize the mapper.
    */
  @Override
  public void setup(Context context) {
    try {
      super.setup(context);
      reader = new TrinidadDocumentReader();

      String acceptedWords = context.getConfiguration().get("wordList");
      if (acceptedWords == null)
        return;

      wordList = new HashSet<String>();
      FileSystem fs = FileSystem.get(context.getConfiguration());
      BufferedReader br = new BufferedReader(new InputStreamReader(
            fs.open(new Path(acceptedWords))));
      for (String line = null; (line = br.readLine()) != null; )
        wordList.add(line.trim());
    }
    catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * Extracts the dependency paths, along with their marking as known or known
   * non Hypernym evidence.
   * 
   * @param key The current key to the document being processed.
   * @param value The current value.
   * @param context The current context.
   * @throws IOException When writing the record fails.
   * @throws InterruptedException When the job is aborted.
   */
  @Override
  public void map(ImmutableBytesWritable key, Result row, Context context)
      throws IOException, InterruptedException {
    context.getCounter("count paths", "seen row").increment(1);

    ReportProgressThread progress = null;
    try {
      progress = ReportProgressThread.start(context, 1000);

      // Get the corpus name and dependency tree for the document.
      String source = reader.getTextSource(row);
      context.getCounter("count paths", source).increment(1);
      DependencyTreeNode[] dependencyTree = reader.getDependencyTree(row);

      // Reject any documents that lack a dependency tree.
      if (dependencyTree == null) {
        return;

        /*
         * Commented out now that the parser is working.
        context.getCounter("Parsed Document", "Parsing").increment(1);
        String text = reader.getText(row);
        AnnotationSet parses = new AnnotationSet(Constants.PARSE);
        AnnotationSet depAnnots = new AnnotationSet(Constants.DEP);

        AnnotationSet posSet = DocSchema.getAnnotationSet(
            row, Constants.POS);
        if (!parserUtil.parseFromDocSchema(
              text, row, parses, depAnnots, posSet)) {
          context.getCounter("Parsed Document", "Bad Parse").increment(1);
          return;
        }

        // Store the data into the row so that it's there, forever.
        if (parses.size() == 0 && depAnnots.size() == 0) {
          context.getCounter("Parsed Document", "Empty Parse").increment(1);
          return;
        }

        Put put = new Put(key.get());

        DocSchema.add(put, DocSchema.annotationsCF, Constants.PARSE,
                      AnnotationUtils.getAnnotationStr(parses));
        DocSchema.add(put, DocSchema.annotationsCF, Constants.DEP, 
                      AnnotationUtils.getAnnotationStr(depAnnots));
        docTable.put(put);

        dependencyTree = TrinidadDocumentReader.extractFromAnnotations(
            text, depAnnots, posSet);
            */
      }

      dependencyTree = DependencyUtil.addConjunctionRelations(dependencyTree);

      for (DependencyTreeNode treeNode : dependencyTree) {
        context.getCounter("count paths", "valid tree").increment(1);

        // Reject any nodes that are not nouns.
        if (!treeNode.pos().startsWith("NN"))
          continue;

        Set<DependencyTreeNode> seenNodes = new HashSet<DependencyTreeNode>();

        // Iterate through all of the available dependency paths starting at
        // this current node.  For all valid paths, i.e. ones that start and
        // end with nounds, and at least one of the nodes is in word net, emit
        // the dependency path between the two nouns.
        Iterator<DependencyPath> pathIter = new FilteredDependencyIterator(
            treeNode, new UniversalPathAcceptor(), 7);
        while (pathIter.hasNext()) {
          DependencyPath path = pathIter.next();

          // Reject any end nodes that are not nouns.
          if (!path.last().pos().startsWith("NN"))
            continue;

          // Reject any words not contained in the word list, if one was
          // created.
          if (wordList != null && !wordList.contains(path.last().word()))
            continue;

          String childTerm = path.first().word();
          String parentTerm = path.last().word();

          // Check to see if the current end node in the path has already been
          // observed.  This allows us to select only the dependency path
          // which is shortest between the two nodes.
          if (seenNodes.contains(path.last()))
            continue;
          seenNodes.add(path.last());

          emitPath(childTerm, parentTerm, source, path, context);

          context.getCounter("count paths", "noun pair").increment(1);
        }
      }
    } finally {
      if (progress != null)
        progress.interrupt();
    } 
  }

  /**
   * Emits "{@code childTerm}|{@code parentTerm};;{@code source}" as the key and
   * a string form of {@code path} as the value.
   *
   * @param childTerm The first term in the dependency path.  This serves as
   *        part the emitted key.
   * @param parentTerm The final term in the dependency path.  This serves as
   *        part of the emitted key.
   * @param source The document collection name that generated this data value.
   *        This served as part of the emitted key
   * @param path The {@link DependencyPath}.  A string form of this path is the
   *        emitted value.
   * @param context The {@link Context} used to emit a key, value pair.
   *
   * @throws IOException
   * @throws InterruptedException
   */
  protected void emitPath(String childTerm, String parentTerm, String source,
                          DependencyPath path, Context context) 
      throws IOException, InterruptedException {
    // Convert the dependency path into a simple string.
    StringBuilder pathStrBuilder = new StringBuilder();
    for (DependencyRelation relation : path)
      pathStrBuilder.append(relation.relation()).append(",");
    String dependencyPath = pathStrBuilder.toString();

    // Emit the noun pair and the relationship path.
    String keyPair = childTerm + NOUN_SEPARATOR + parentTerm +
                     SOURCE_SEPARATOR + source;
    context.write(new Text(keyPair), new Text(dependencyPath));
  }

  /**
   * {@inheritDoc}
   */
  protected void cleanup(Context context) 
      throws IOException, InterruptedException {
    super.cleanup(context);

    /*
     * Commented out since the parse is currently working.
    docTable.flushCommits();
    docTable.close();
    */

    reader.close(); 
  }
}
