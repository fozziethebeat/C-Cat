
package gov.llnl.ontology.wordnet;


import java.io.File;
import java.util.List;

/**
 * An {@link Attribute} that stores a list of java.io.Files
 */
public class FileListAttribute<T extends List<File>>
    implements Attribute<T> {

  /**
   * The list of java.io.File objects being stored.
   */
  private T files;

  /**
   * Creates a new {@link FileListAttribute}.
   */
  public FileListAttribute(T listOfFiles) {
    this.files = listOfFiles;
  }

  /**
   * {@inheritDoc}
   */
  public void merge(Attribute<T> other) {
      files.addAll(other.object());
  }

  /**
   * {@inheritDoc}
   */
  public T object() {
    return files;
  }


  public String toString() {
      return files.toString();

  }
}

