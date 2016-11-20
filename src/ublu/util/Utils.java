/*
 * Copyright (c) 2014, Absolute Performance, Inc. http://www.absolute-performance.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package ublu.util;

import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.PrintObject;
import com.ibm.as400.access.Printer;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import ublu.command.Command;
import ublu.command.CommandInterface;

/**
 * Misc util routines
 *
 * @author jwoehr
 */
public class Utils {

    /**
     * Grab whole contents of a file as one string, limited by String size.
     *
     * @param filepath path to file
     * @return contents of file as string or null
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static String getFileAsString(String filepath) throws FileNotFoundException, IOException {
        StringBuilder sb = new StringBuilder();
        FileReader fileReader = new FileReader(new File(filepath));
        try (BufferedReader bufferedReader = new BufferedReader(fileReader)) {
            while (bufferedReader.ready()) {
                sb.append(bufferedReader.readLine());
            }
        }
        return sb.toString();
    }

    /**
     * Split lines for readability Modified from an example:
     * http://stackoverflow.com/questions/7528045/large-string-split-into-lines-with-maximum-length-in-java
     *
     * @param input the string to split
     * @param maxLineLength max length of a line
     * @param tabsin new line indent tabs
     * @param spacesin spaces to add after tab indent
     * @return the newlined string
     */
    public static String breakLines(String input, int maxLineLength, int tabsin, int spacesin) {
        final char NEWLINE = '\n';
        final String SPACE_SEPARATOR = " ";
        final String SPLIT_REGEXP = "\\s+";
        String[] tokens = input.split(SPLIT_REGEXP);
        StringBuilder output = new StringBuilder(input.length());
        int lineLen = 0;
        for (int i = 0; i < tokens.length; i++) {
            String word = tokens[i];

            if (lineLen + (SPACE_SEPARATOR + word).length() > maxLineLength) {
                if (i > 0) {
                    output.append(NEWLINE);
                    for (int x = 0; x < tabsin; x++) {
                        output.append('\t');
                    }
                    for (int x = 0; x < spacesin; x++) {
                        output.append(' ');
                    }
                }
                lineLen = 0;
            }
            if (i < tokens.length - 1 && (lineLen + (word + SPACE_SEPARATOR).length() + tokens[i + 1].length()
                    <= maxLineLength)) {
                word += SPACE_SEPARATOR;
            }
            output.append(word);
            lineLen += word.length();
        }
        return output.toString();
    }

    /**
     * Convert print attribute to integer
     *
     * @param attrName attrib name
     * @return int for attrib
     * @throws NoSuchFieldException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public static int attrNameToInt(String attrName) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        return PrintObject.class.getField(attrName).getInt(Printer.class);
    }

    /**
     * Convert print attribute name to its value
     *
     * @param p print object with attrib
     * @param attrName name of attrib
     * @param c command calling this static method
     * @return value of attribute
     */
    public static Object attrNameToValue(PrintObject p, String attrName, Command c) {
        Object value = null;
        Integer attrInteger = null;
        try {
            attrInteger = attrNameToInt(attrName);
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ex) {
            c.getLogger().log(Level.SEVERE, "No such attribute " + attrName + " in " + c.getNameAndDescription(), ex);
            c.setCommandResult(CommandInterface.COMMANDRESULT.FAILURE);
        }
        if (attrInteger != null) {
            try {
                value = p.getSingleIntegerAttribute(attrInteger);
            } catch (IllegalArgumentException | AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | RequestNotSupportedException ex) {
                try {
                    value = p.getSingleFloatAttribute(attrInteger);
                } catch (IllegalArgumentException | AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | RequestNotSupportedException ex1) {
                    try {
                        value = p.getStringAttribute(attrInteger);
                    } catch (IllegalArgumentException | AS400SecurityException | ErrorCompletingRequestException | IOException | InterruptedException | RequestNotSupportedException ex2) {
                        c.getLogger().log(Level.SEVERE, "Could not get attribute value for " + attrName + " in " + c.getNameAndDescription(), ex);
                        c.setCommandResult(CommandInterface.COMMANDRESULT.FAILURE);
                    }
                }
            }
        }
        return value;
    }
}
