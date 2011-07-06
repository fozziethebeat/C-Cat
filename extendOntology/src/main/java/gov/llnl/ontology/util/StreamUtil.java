package gov.llnl.ontology.util;

import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOError;
import java.io.IOException;

import java.net.URISyntaxException;

import java.util.zip.GZIPInputStream;


/**
 * @author Keith Stevens
 */
public class StreamUtil {

    public static InputStream fromJar(Class c, String filePath) {
        return c.getClassLoader().getResourceAsStream(filePath);
    }

    public static File fileFromJar(Class c, String filePath) {
        try {
            return new File(c.getClassLoader().getResource(filePath).toURI());
        } catch (URISyntaxException use) {
            use.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    public static DataInputStream gzipInputStream(InputStream in) {
        try {
            return new DataInputStream(new GZIPInputStream(in));
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }
}

