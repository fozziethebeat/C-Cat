package gov.llnl.ontology.util;

import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;


/**
 * @author Keith Stevens
 */
public class StringCounter extends Counter<String>
                           implements Writable {

    public StringCounter() {
    }

    public StringCounter(Collection<String> items) {
        super(items);
    }

    public void write(DataOutput out) throws IOException {
        out.writeInt(size());
        for (Map.Entry<String, Integer> entry : this) {
            out.writeUTF(entry.getKey());
            out.writeInt(entry.getValue());
        }
    }

    public void readFields(DataInput in) throws IOException {
        int size = in.readInt();
        for (int i = 0; i < size; ++i) {
            String key = in.readUTF();
            int value = in.readInt();
            count(key, value);
        }
    }
}
