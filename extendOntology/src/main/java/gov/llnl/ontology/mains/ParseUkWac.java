/*
 * Copyright (c) 2011, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
 *
 * This file is part of the C-Cat package and is covered under the terms and
 * conditions therein.
 *
 * The C-Cat package is free software: you can redistribute it and/or modify
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

package gov.llnl.ontology.mains;

import gov.llnl.ontology.text.parse.Parser;
import gov.llnl.ontology.text.parse.MaltLinearParser;
import gov.llnl.ontology.text.parse.MaltSvmParser;

import gov.llnl.text.util.FileUtils;

import java.io.PrintWriter;


/**
 * A simple main to test that the malt parser can be loaded up into memory
 * correctly and used to re-parse a wacky corpus.
 *
 * @author Keith Stevens
 */
public class ParseUkWac {

    public static void main(String[] args) throws Exception {
        PrintWriter writer = new PrintWriter(args[0]);
        Parser parser;
        if (args[1].equals("s"))
            parser = new MaltSvmParser();
        else 
            parser = new MaltLinearParser();

        StringBuilder sentence = new StringBuilder();
        for (int i = 2; i < args.length; ++i) {
            for (String line : FileUtils.iterateFileLines(args[i])) {
                if (line.startsWith("<text") ||
                    line.startsWith("<s") ||
                    line.startsWith("</text") ||
                    line.length() == 0 )
                    writer.println(line);
                else if (line.startsWith("</s")) {
                    writer.println(parser.parseText("", sentence.toString()));
                    sentence = new StringBuilder();
                } else
                    sentence.append(line.split("\\s+")[0]).append(" ");
            }
        }
        writer.close();
    }
}

