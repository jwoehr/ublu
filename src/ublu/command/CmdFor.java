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
package ublu.command;

import ublu.util.ArgArray;
import ublu.util.Generics.StringArrayList;
import ublu.util.Parser;
import ublu.util.Tuple;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.JobList;
import com.ibm.as400.access.ObjectDoesNotExistException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.logging.Level;

/**
 *
 * Iterate over an enumerable instancing an instance var
 *
 * @author jwoehr
 */
public class CmdFor extends Command {

    {
        setNameAndDescription("FOR", "/5 @iteratorvar ~@valuevar $[ cmd .. ]$ : FOR enumerable @valuevar execute block instancing @iteratorvar");
    }

    /* Parse and execute a FOR block
     *
     * @param argArray args to the interpreter
     * @return what's left of the args
     */
    /**
     * Do the work of a FOR cmd
     *
     * @param argArray passed-in arg array
     * @return remnant of the arg array
     */
    public ArgArray doCmdFor(ArgArray argArray) {
        getInterpreter().pushLocal(); // local context for iterator
        Tuple iteratorTuple;
        if (!argArray.isNextTupleName()) {
            getLogger().log(Level.SEVERE, "Iterator tuple name " + argArray.next() + " is invalid in {0}", getNameAndDescription());
            setCommandResult(COMMANDRESULT.FAILURE);
        } else {
            String iteratorTupleName = argArray.next();
            iteratorTuple = new Tuple(iteratorTupleName, null);
            getInterpreter().putTupleMostLocal(iteratorTuple);
            if (argArray.peekNext().equalsIgnoreCase("in")) {
                argArray.next(); // discard "in"
            }
            Tuple iteratedTuple = argArray.nextTupleOrPop();
            if (iteratedTuple == null) {
                getLogger().log(Level.SEVERE, "Iterated tuple does not exist in {0}", getNameAndDescription());
                setCommandResult(COMMANDRESULT.FAILURE);
            } else {
                String block = argArray.nextUnlessNotBlock();
                if (block == null) {
                    getLogger().log(Level.SEVERE, "FOR found without a $[ block ]$");
                    setCommandResult(COMMANDRESULT.FAILURE);
                } else {
                    Parser p = new Parser(getInterpreter(), block);
                    ArgArray aa = p.parseAnArgArray();
                    getInterpreter().pushFrame();
                    getInterpreter().setForBlock(true);
                    walkFor(iteratedTuple.getValue(), aa, iteratorTuple);
                    if (getInterpreter().isBreakIssued()) {
                        // If a BREAK then the frame was already popped
                        getInterpreter().setBreakIssued(false);
                    } else {
                        getInterpreter().popFrame();
                    }
                }
            }
        }
        getInterpreter().popLocal();
        return argArray;
    }

    private void walkFor(Object o, ArgArray argArray, Tuple iteratorTuple) {
        if (o != null) {
            if (o instanceof JobList) {
                walk(JobList.class.cast(o), argArray, iteratorTuple);
            }
            if (o instanceof Iterable) {
                walk(Iterable.class.cast(o), argArray, iteratorTuple);
            }
            if (o instanceof String) {
                walk(String.class.cast(o), argArray, iteratorTuple);
            }
        }
    }

    private void walk(Iterable it, ArgArray argArray, Tuple iteratorTuple) {
        Iterator i = it.iterator();
        ArgArray copy;
        while (i.hasNext() && !getInterpreter().isBreakIssued()) {
            copy = new ArgArray(getInterpreter(), argArray);
            getInterpreter().setArgArray(copy);
            iteratorTuple.setValue(i.next());
            setCommandResult(getInterpreter().loop());
            if (getCommandResult() == COMMANDRESULT.FAILURE) {
                break;
            }
        }
    }

    private void walk(String s, ArgArray argArray, Tuple iteratorTuple) {
        StringArrayList sal = new StringArrayList(s);
        walk(sal, argArray, iteratorTuple);
    }

    private void walk(JobList jl, ArgArray argArray, Tuple iteratorTuple) {
        Enumeration jobs = null;
        ArgArray copy;
        try {
            jobs = jl.getJobs();
        } catch (AS400SecurityException | ErrorCompletingRequestException | InterruptedException | IOException | ObjectDoesNotExistException ex) {
            getLogger().log(Level.SEVERE, "Error fetching job list", ex);
            setCommandResult(COMMANDRESULT.FAILURE);
        }
        if (jobs != null) {
            while (jobs.hasMoreElements()
                    && getInterpreter().isForBlock()
                    && !getInterpreter().isBreakIssued()) {
                copy = new ArgArray(getInterpreter(), argArray);
                getInterpreter().setArgArray(copy);
                iteratorTuple.setValue(jobs.nextElement());
                setCommandResult(getInterpreter().loop());
                if (getCommandResult() == COMMANDRESULT.FAILURE) {
                    break;
                }
            }
        }
    }

    @Override
    public ArgArray cmd(ArgArray args) {
        reinit();
        return doCmdFor(args);
    }

    @Override
    public CommandInterface.COMMANDRESULT getResult() {
        return getCommandResult();
    }
}
