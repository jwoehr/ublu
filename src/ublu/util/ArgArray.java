/*
 * Copyright (c) 2014, Absolute Performance, Inc. http://www.absolute-performance.com
 * Copyright (c) 2016, Jack J. Woehr jwoehr@softwoehr.com 
 * SoftWoehr LLC PO Box 51, Golden CO 80402-0051 http://www.softwoehr.com
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * The command line space-delimited args lexed into string array elements to be
 * passed into {@link ublu.util.Interpreter#loop()}
 *
 * @author jwoehr
 */
public class ArgArray extends ArrayList<String> {

    private Interpreter myInterpreter;

    /**
     * Get associated Interpreter instance
     *
     * @return associated Interpreter instance
     */
    public Interpreter getInterpreter() {
        return myInterpreter;
    }

    /**
     * Set associated Interpreter instance
     *
     * @param interpreter associated Interpreter instance
     */
    private void setInterpreter(Interpreter interpreter) {
        this.myInterpreter = interpreter;
    }
    /**
     * The char string introducing a quoted string in our application-specific
     * language.
     */
    public static final String OPENQUOTE = "${";
    /**
     * The char string closing a quoted string in our application-specific
     * language
     */
    public static final String CLOSEQUOTE = "}$";
    private static final String OPENBLOCK = "$[";
    private static final String CLOSEBLOCK = "]$";
    /**
     * Character which represents in the arg array popping a tuple from the
     * tuple stack
     */
    public static final String POPTUPLE = "~";

    /**
     * Instance empty
     */
    private ArgArray() {
    }

    /**
     * Instance from just an Interpreter
     *
     * @param i associated Interpreter
     */
    public ArgArray(Interpreter i) {
        setInterpreter(i);
    }

    /**
     * Instance from a string collection
     *
     * @param i associated Interpreter
     * @param c a string collection
     */
    public ArgArray(Interpreter i, Collection<? extends String> c) {
        super(c);
        setInterpreter(i);
    }

    /**
     * Instance from string array
     *
     * @param i associated Interpreter
     * @param args the string array containing args (i.e., a l-to-r list of
     * commands to execute)
     */
    public ArgArray(Interpreter i, String[] args) {
        super();
        addAll(Arrays.asList(args));
        setInterpreter(i);
    }

    /**
     * Return (removing) the next lex in the arg array. This is how we walk
     * through each line of command input.
     *
     * @return the next lex in the arg array as a string
     */
    public String next() {
        return this.remove(0);
    }

    /**
     * Take a look at next lex without removing it
     *
     * @return next lex without removing it
     */
    public String peekNext() {
        return get(0);
    }

    /**
     * True .IFF. next item in arg array is a tuple name and a tuple isn't
     * mapped with that name
     *
     * @return True .IFF. next item in arg array is a tuple name and a tuple
     * isn't mapped with that name
     */
    public boolean peekNonExistentTuple() {
        boolean result = false;
        String s = peekNext();
        if (s != null) {
            s = s.trim();
            if (Tuple.isTupleName(s)) {
                result = getInterpreter().getTuple(s) == null;
            }
        }
        return result;
    }

    /**
     * Return (removing) next lex in the arg array as an int.
     *
     * @return int value of next lex in the arg array
     */
    public int nextInt() {
        return Long.decode(next()).intValue();
        // return Integer.parseInt(next());
    }

    /**
     * Lookahead and see if the next lex in the arg array is a dash-command.
     *
     * @return true if the next lex in the arg array is a dash-command
     */
    public boolean hasDashCommand() {
        return !this.isEmpty() && this.get(0).startsWith("-");
    }

    /**
     * Lookahead for a dash-command and return one (removing) if found. If no
     * dash-command found, return the empty string.
     *
     * @return the dash command or an empty string
     */
    public String parseDashCommand() {
        String dashCommand = "";
        if (hasDashCommand()) {
            dashCommand = this.next();
        }
        return dashCommand;
    }

    /**
     * Return (removing) the next <i>n</i> lexes as an array of ints.
     *
     * <p>
     * Some dash-commands are followed by an int count of the following ints and
     * then the ints themselves, providing the ability to parse an array of ints
     * from the ArgArray of lexes.</p>
     *
     * @return an array of ints
     */
    public int[] parseIntArray() {
        int[] result = new int[0];
        int entries = nextInt();
        if (entries > 0) {
            result = new int[entries];
            for (int i = 0; i < entries; i++) {
                result[i] = nextInt();
            }
        }
        return result;
    }

    private int findOpener(String opener) {
        int index = -1;
        Iterator it = iterator();
        for (int i = 0; it.hasNext(); i++) {
            if (opener.equals(it.next())) {
                index = i;
                break;
            }
        }
        return index;
    }

    /**
     * Lookahead to find the next open quote ( <tt>${</tt> ) in the ArgArray of
     * lexes.
     *
     * @return the zero-based index in the ArgArray of the open-quote
     */
    public int findOpenQuote() {
        return findOpener(OPENQUOTE);
    }

    /**
     * Lookahead to find the next openblock ( <tt>${</tt> ) in the ArgArray of
     * lexes.
     *
     * @return the zero-based index in the ArgArray of the openblock
     */
    public int findOpenBlock() {
        return findOpener(OPENBLOCK);
    }

    /**
     * Looks to see if the next lex in the ArgArray is an openquote.
     *
     * @return true if the next lex in the ArgArray is an openquote
     */
    public boolean isOpenQuoteNext() {
        return findOpenQuote() == 0;
    }

    /**
     * Looks to see if the next lex in the ArgArray is an openblock.
     *
     * @return true if the next lex in the ArgArray is an openblock
     */
    public boolean isOpenBlockNext() {
        return findOpenBlock() == 0;
    }

    private int findCloser(int startIndex, String closer) {
        int index = -1;
        for (int i = startIndex; i < size(); i++) {
            if (closer.equals(get(i))) {
                index = i;
                break;
            }
        }
        return index;
    }

    /**
     * Lookahead to find the next closequote.
     *
     * @param startIndex
     * @return zero-based index in the ArgArray of lexes of the next closequote,
     * or -1 if not found.
     */
    public int findCloseQuote(int startIndex) {
        return findCloser(startIndex, CLOSEQUOTE);
    }

    /**
     * Lookahead to find the next closeblock.
     *
     * @param startIndex index in ArgArray to start at
     * @return zero-based index in the ArgArray of lexes of the next closeblock,
     * or -1 if not found.
     */
    public int findCloseBlock(int startIndex) {
        int index = -1;
        for (int i = startIndex; i < size(); i++) {
            if (OPENBLOCK.equals(get(i).trim())) {
                // increment count of nested blocks
                // /* Debug */ System.err.println("Found OPENBLOCK at " + i);
                getInterpreter().setParsingBlock(true);
            }
            if (CLOSEBLOCK.equals(get(i).trim())) {
                // decrement count of nested blocks
                // /* Debug */ System.err.println("Found CLOSEBLOCK at " + i);
                getInterpreter().setParsingBlock(false);
            }
            if (!getInterpreter().isParsingBlock()) {
                // Down to zero (0) nested blocks? Done.
                index = i;
                break;
            }
        }
        return index;
    }

    /**
     * Packs all lexes from the ArgArray between the open and close quote into
     * one lex at the quoted string's current first element location and shrinks
     * the ArgArray down correspondingly.
     *
     * @param openQuoteIndex index in the ArgArray of the openquote
     * @param closeQuoteIndex index in the ArgArray of the closequote
     */
    public void packQuotation(int openQuoteIndex, int closeQuoteIndex) {
        StringBuilder sb = new StringBuilder();
        ArgArray theQuote = new ArgArray(getInterpreter(), subList(openQuoteIndex + 1, closeQuoteIndex));
        removeRange(openQuoteIndex, closeQuoteIndex);
        for (String s : theQuote) {
            sb.append(s).append(" ");
        }
        set(openQuoteIndex, sb.toString());
    }

    /**
     * Packs all lexes from the ArgArray between the open and close block into
     * one lex at the quoted string's current first element location and shrinks
     * the ArgArray down correspondingly.
     *
     * @param openBlockIndex index in the ArgArray of the openquote
     * @param closeBlockIndex index in the ArgArray of the closequote
     */
    public void packBlock(int openBlockIndex, int closeBlockIndex) {
        StringBuilder sb = new StringBuilder();
        ArgArray theBlock = new ArgArray(getInterpreter(), subList(openBlockIndex + 1, closeBlockIndex));
        removeRange(openBlockIndex, closeBlockIndex);
        for (String s : theBlock) {
            sb.append(s).append(" ");
        }
        set(openBlockIndex, sb.toString());
    }

    /**
     * Parse in pursuit of the close quote of a quoted string even if it means
     * prompting the user and grabbing more input until the close quote is
     * found. Pack the resulting quotation as the next element in the ArgArray.
     *
     */
    public void assimilateFullQuotation() {
        while (findCloseQuote(0) == -1) {
            getInterpreter().setParsingString(true);
            getInterpreter().prompt();
            addAll(getInterpreter().readAndParse());
            getInterpreter().setParsingString(false);
        }
        packQuotation(0, findCloseQuote(0));
    }

    /**
     * Parse in pursuit of the close block of a block even if it means prompting
     * the user and grabbing more input until the close block is found. Pack the
     * resulting block as the next element in the ArgArray.
     *
     */
    public void assimilateFullBlock() {
        int searchStartIndex = 0;
        // /* Debug */ System.err.println("block depth before " + getInterpreter().getParsingBlockDepth());
        while (findCloseBlock(searchStartIndex) == -1) {
            searchStartIndex = size();
            // /* Debug */ System.err.println("block depth at begin loop " + getInterpreter().getParsingBlockDepth());
            getInterpreter().prompt();
            addAll(getInterpreter().readAndParse());
            // /* Debug */ System.err.println("block depth at end " + getInterpreter().getParsingBlockDepth());
        }
        // /* Debug */ System.err.println("block depth after " + getInterpreter().getParsingBlockDepth());
        packBlock(0, findCloseBlock(0));
    }

    /**
     * True if a quotation is next in this arg array
     *
     * @return True iff a quotation is next in this arg array
     */
    public boolean isNextQuotation() {
        return peekNext().equals(OPENQUOTE);
    }

    /**
     * True iff next in this arg array is a tuple name
     *
     *
     * @return True iff next in this arg array is a tuple name
     */
    public boolean isNextTupleName() {
        return Tuple.isTupleName(peekNext());
    }

    private boolean isNextPopTuple() {
        return peekNext().equals(POPTUPLE);
    }

    /**
     * Return true iff next in this arg array is a tuple name or the
     * "pop-the-tuple-stack" symbol
     *
     * @return true iff next in this arg array is a tuple name or the
     * "pop-the-tuple-stack" symbol
     */
    public boolean isNextTupleNameOrPop() {
        return isNextTupleName() || isNextPopTuple();
    }

    /**
     * Flag if next lex is of form of a constant name, i.e. *name.
     *
     * @return true .iff. next lex is of form of a constant name, i.e. *name
     * @see ublu.util.Const
     */
    public boolean isNextConstName() {
        return Const.isConstName(peekNext());
    }

    /**
     * True iff a block is next in this arg array
     *
     * @return True iff a block is next in this arg array
     */
    public boolean isNextBlock() {
        return peekNext().equals(OPENBLOCK);
    }

    /**
     * True iff next lex is THEN
     *
     * @return True iff next lex is ELSE
     */
    public boolean isNextThen() {
        return peekNext().equals("THEN");
    }

    /**
     * True iff next lex is ELSE
     *
     * @return True iff next lex is ELSE
     */
    public boolean isNextElse() {
        return peekNext().equals("ELSE");
    }

    /**
     * Look up next arg as a tuple name
     *
     * @return The tuple or null if not found
     */
    public Tuple nextTuple() {
        return getInterpreter().getTuple(next());
    }

    /**
     * Look up next arg as a tuple name or a popped tuple from stack
     *
     * @return The tuple or null if not found
     */
    public Tuple nextTupleOrPop() {
        Tuple t = null;
        if (isNextPopTuple()) {
            next(); // discard "~" symbol
            t = getInterpreter().getTupleStack().pop();
        } else if (isNextTupleName()) {
            t = getInterpreter().getTuple(next());
        } else {
            next(); // discard non-tuple
        }
        return t;
    }

    /**
     * Look up next arg as a tuple name or a popped tuple from stack and return
     * its Boolean value if Boolean or null if not.
     *
     * @return The tuple's Boolean value or null if not found or not Boolean
     */
    public Boolean nextBooleanTupleOrPop() {
        Boolean result = null;
        Tuple t = null;
        if (isNextPopTuple()) {
            next(); // discard "~" symbol
            t = getInterpreter().getTupleStack().pop();
        } else if (isNextTupleName()) {
            t = getInterpreter().getTuple(next());
        } else {
            next(); // discard non-tuple
        }
        if (t != null) {
            Object o = t.getValue();
            if (o instanceof Boolean) {
                result = Boolean.class.cast(o);
            }
        }
        return result;
    }

    /**
     * Check if the next element in the ArgArray is the openquote and if so
     * assimilate the quotation before returning the next element in the
     * ArgArray.
     *
     * @return next element in the ArgArray, possibly an assimilated quotation
     */
    public String nextMaybeQuotation() {
        if (isNextQuotation()) {
            assimilateFullQuotation();
        }
        return next();
    }

    /**
     * Check if the next element in the ArgArray is a tuplename, and if so,
     * return the string value of the tuple. If it's not a tuple, look for the
     * the openquote and if so assimilate the quotation before returning the
     * next element in the ArgArray.
     *
     * @return next element in the ArgArray, possibly a string from a tuple or
     * an assimilated quotation. Value could be null from a tuple
     */
    public String nextMaybeQuotationTupleString() {
        String result = null;
        if (isNextTupleName()) {
            String tupleName = next();
            Tuple t = getInterpreter().getTuple(tupleName);
            if (t != null) {
                Object o = t.getValue();
                result = o == null ? null : t.getValue().toString();
            }
        } else {
            if (isNextQuotation()) {
                assimilateFullQuotation();
            }
            result = next();
        }
        return result;
    }

    /**
     * Check if the next element in the ArgArray is a tuplename or the symbol
     * POPTUPLE ("~"), and if so, return the string value of the tuple. If it's
     * not a tuple (popped or otherwise), look for the the openquote and if so
     * assimilate the quotation before returning the next element in the
     * ArgArray whether it was a quotation or just a plain word.
     *
     * @return next element in the ArgArray, possibly a string from a tuple
     * (popped or otherwise) or an assimilated quotation. Value could be null
     * from a tuple
     */
    public String nextMaybeQuotationTuplePopString() {
        String result = null;
        if (isNextConstName() && getInterpreter().getConst(peekNext()) != null) {
            result = getInterpreter().getConst(next());
        } else if (isNextTupleNameOrPop()) {
            Tuple t;
            if (isNextPopTuple()) {
                t = getInterpreter().getTupleStack().pop();
                next(); // discard "~"
            } else {
                t = getInterpreter().getTuple(next());
            }
            if (t != null) {
                Object o = t.getValue();
                result = o == null ? null : t.getValue().toString();
            }
        } else {
            if (isNextQuotation()) {
                assimilateFullQuotation();
            }
            result = next();
        }
        return result;
    }

    /**
     * Check if the next element in the ArgArray is a tuple or an unadorned lex
     * and return either the tuple value or the String lex.
     *
     * @return String frome next element in the ArgArray, either the undecorated
     * lex or the String value of the Tuple value if the lex is decorated as a
     * Tuple name, i.e., begins with the "at" sign.
     */
    public String nextMaybeTupleString() {
        String result = null;
        if (isNextTupleName()) {
            String tupleName = next();
            Tuple t = getInterpreter().getTuple(tupleName);
            if (t != null) {
                Object o = t.getValue();
                result = o == null ? null : t.getValue().toString();
            }
        } else {
            result = next();
        }
        return result;
    }

    /**
     * Check if the next element in the ArgArray is a tuple or an unadorned lex
     * and return either the tuple value or the String lex parsed as an int
     *
     * @return int whose value was represented either by the next lex in the arg
     * array or by the tuple whose name was the next lex in the arg array.
     */
    public int nextIntMaybeTupleString() {
        return Long.decode(nextMaybeTupleString()).intValue();
        // return Integer.parseInt(nextMaybeTupleString());
    }

    /**
     * Check if the next element in the ArgArray is a tuplename or the symbol
     * POPTUPLE ("~"), and if so, return the int value of the tuple. If it's not
     * a tuple (popped or otherwise), look for the the openquote and if so
     * assimilate the quotation before returning the next element in the
     * ArgArray whether it was a quotation or just a plain word and return it
     * parsed as an int.
     *
     * @return int whose value was represented either by the next lex in the arg
     * array or by the tuple popped or whose name was the next lex in the arg
     * array.
     */
    public int nextIntMaybeQuotationTuplePopString() {
        return Long.decode(nextMaybeQuotationTuplePopString()).intValue();
    }

    /**
     * Check if the next element in the ArgArray is the openquote and if so
     * assimilate the quotation but only return the next element in the ArgArray
     * if there was indeed a quotation.
     *
     * @return next element if it was a quotation, or null if next would not be
     * a quotation
     */
    public String nextUnlessNotQuotation() {
        String s = null;
        if (isNextQuotation()) {
            assimilateFullQuotation();
            s = next();
        }
        return s;
    }

    /**
     * Check if the next element in the ArgArray is the openblock and if so
     * assimilate the block but only return the next element in the ArgArray if
     * there was indeed a block.
     *
     * @return next element (removing it) .IFF. it was a block, or null if next
     * would not be a block (without removing the non-block whatever-it-is from
     * the arg array).
     */
    public String nextUnlessNotBlock() {
        String s = null;
        if (isNextBlock()) {
            //interpreter.getArgArray().remove(0); // throw away the block opener!
            assimilateFullBlock();
            s = next();
        }
        return s;
    }

    /**
     * Reconstruct something resembling the original command line as a string
     *
     * @return something resembling the original command line
     */
    public String toHistoryLine() {
        StringBuilder sb = new StringBuilder();
        for (String s : this) {
            sb.append(s).append(" ");
        }
        return sb.toString();
    }
}
