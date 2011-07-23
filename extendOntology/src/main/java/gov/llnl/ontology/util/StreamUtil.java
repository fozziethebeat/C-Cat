package gov.llnl.ontology.util;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOError;
import java.io.IOException;

import java.net.URISyntaxException;

import java.util.zip.GZIPInputStream;


/**
 * This collects a set of commonly used methods accessing data streams and files
 * in and outside of running jars.
 *
 * @author Keith Stevens
 */
public class StreamUtil {

    /**
     * Returns an {@link InputStream} corresponding to the the file denoted by
     * {@code filePath}.  It will be read from the current set of class
     * resources accessible by the given running class's classpath.  This
     * classpath has access to not only local files via an absolute path, but
     * also files embedded within a jar.
     *
     * @param c The current {@link Class} that requires a resource
     * @param filePath The name of the file which should be read
     * @return A {@link InputStream} for data in {@code filePath}
     */
    public static InputStream fromJar(Class c, String filePath) {
        return c.getClassLoader().getResourceAsStream(filePath);
    }

    /**
     * Returns a {@link InputStream} corresponding to the absolute path name
     * denoted by {@code filePath}.  This will not access any files within the
     * currently running jar or class path.
     *
     * @param filePath The name of the file which should be read
     * @return A {@link InputStream} for data in {@code filePath}
     */
    public static InputStream fromPath(String filePath) {
        try {
            return new FileInputStream(filePath);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Returns a {@link File} object associated with {@link filePath} located
     * within the current running jar that has loaded {@link Class} {@code c}.
     * The returned {@link File} cannot be read directly but my contain valuable
     * meta data.
     *
     * @param c The current {@link Class} that requires a resource
     * @param filePath The name of the file which should be opened 
     * @return A {@link File} for data in {@code filePath}
     */
    public static File fileFromJar(Class c, String filePath) {
        try {
            return new File(c.getClassLoader().getResource(filePath).toURI());
        } catch (URISyntaxException use) {
            use.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    /**
     * Returns a {@link DataInputStream} for reading object data from a gzipped
     * {@link InputStream}.
     *
     * @param in An existing {@link InputStream} whose data is in the gzipped
     *        format
     * @return A {@link DataInputStream} for reading data from the gzipped
     *         stream
     */
    public static DataInputStream gzipInputStream(InputStream in) {
        try {
            return new DataInputStream(new GZIPInputStream(in));
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }
}

