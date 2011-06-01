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

package gov.llnl.ontology.table;

import com.google.common.collect.Maps;

import edu.ucla.sspace.dependency.DependencyTreeNode;
import edu.ucla.sspace.dependency.SimpleDependencyTreeNode;
import edu.ucla.sspace.dependency.DependencyRelation;
import edu.ucla.sspace.dependency.SimpleDependencyRelation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import org.apache.hadoop.mapreduce.Counter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;

import java.util.Map;


/**
 * A set of often used methods for accessing values in a HBase table.
 *
 * @author David Buttler
 * @author Keith Stevens
 */
public class SchemaUtil {

    private static final Log LOG = LogFactory.getLog(SchemaUtil.class);

    /**
     * Returns the text of a given column.
     */
    public static String getColumn(Result row, String fieldName) {
        String[] familyAndQualifier = fieldName.split(":");
        if (familyAndQualifier.length != 2)
            return null;
        return getColumn(row, familyAndQualifier[0], familyAndQualifier[1]);
    }

    /**
     * Returns the text for a given column.
     */
    public static String getColumn(Result row,
                                   String family,
                                   String qualifier) {
        try {
            // Get the raw text
            byte[] bytes = row.getValue(
                    family.getBytes(), qualifier.getBytes());
            if (bytes == null) 
                return null;
            String text = new String(bytes, HConstants.UTF8_ENCODING);
            return text;
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a double value extracted from the specified column.
     */
    public static Double getDoubleColumn(Result row,
                                         String family,
                                         String qualifier) {
        // Get the raw text
        byte[] bytes = row.getValue(family.getBytes(), qualifier.getBytes());

        // Return null for non existing values.
        if (bytes == null || bytes.length == 0)
            return null;

        return Bytes.toDouble(bytes);
    }

    /**
     * Returns the {@link Object} stored in this row at the given column family
     * and column qualifier.  If no data exists, this returns {@code null}.
     */
    public static <T> T getObjectColumn(Result row, String col, String qual) {
        try {
            byte[] bytes = row.getValue(col.getBytes(), qual.getBytes());
            if (bytes == null)
                return null;
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream dis = new ObjectInputStream(bis);
            Object value = dis.readObject();
            dis.close();
            bis.close();
            return value;
        } catch (IOException ioe) {
            throw new IOError(ioe);
        } catch (ClassNotFoundException cnfe) {
            throw new IOError(cnfe);
        }
    }

    /**
     * Stores the given {@code Object} into the {@link Put} at the specified
     * column family and column qualifier.  {@code object} should implement
     * {@link Serializable}.
     */
    public static void add(Put put, String col, String qual, Object object) {
        try {
            if (object == null)
                return;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream dos = new ObjectOutputStream(bos);
            dos.writeObject(object);
            dos.close();
            bos.close();
            put.add(col.getBytes(), qual.getBytes(), bos.toByteArray());
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Adds data to a {@link Put} under a specified column and qualifier.
     */
    public static boolean add(Put put, String col, String qual, String data) {
        if (data == null || data.trim().equals(""))
            return false;
        put.add(col.getBytes(), qual.getBytes(), data.getBytes());
        return true;
    }

    /**
     * Adds data to a {@link Put} under a specified column and qualifier.
     * Returns false if the add failed.
     */
    public static boolean add(Put put, String col, String qual, 
                              String data, Counter counter) {
        if (!add(put, col, qual, data))
            return false;
        counter.increment(1);
        return true;
    }

    /**
     * Adds data to a {@link Put} under a specified column and qualifier.
     */
    public static void add(Put put, String col, String qual, Double value) {
        if (value == null)
            value = 0.0;
        put.add(col.getBytes(), qual.getBytes(), Bytes.toBytes(value));
    }

    /**
     * Creates a new column family for the table with default versions, in
     * memory columns, block cache, and TTL.
     */
    public static void addDefaultColumnFamily(HTableDescriptor tableDescriptor,
                                              byte[] columnFamilyName) {
        tableDescriptor.addFamily(new HColumnDescriptor(
                    columnFamilyName.getBytes(), DEFAULT_VERSIONS, "GZ", 
                    DEFAULT_IN_MEMORY, DEFAULT_BLOCKCACHE, 
                    DEFAULT_TTL, DEFAULT_BLOOMFILTER));
    }
}
