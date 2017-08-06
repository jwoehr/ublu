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
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Puts objects to a {@link DataSink}.
 * <p>
 * A Putter can put Objects (including strings) to an output sink. Sometimes
 * that sink is standard out, sometimes it is a file, sometimes it is a tuple
 * var. This allows us to treat a {@code String} the same as a
 * {@code ResultSet}.</p>
 *
 * @author jwoehr
 */
public class Putter {

    private Object object;
    private Interpreter interpreter;
    private String charsetName;

    /**
     * Get name of charset we are using
     *
     * @return name of charset we are using
     */
    public String getCharsetName() {
        return charsetName;
    }

    /**
     * Set name of charset we are using
     *
     * @param charsetName name of charset we are using
     */
    protected final void setCharsetName(String charsetName) {
        this.charsetName = charsetName;
    }

    /**
     * Get Interpreter instance we are serving
     *
     * @return Interpreter instance we are serving
     */
    public Interpreter getInterpreter() {
        return interpreter;
    }

    /**
     * Set Interpreter instance we are serving
     *
     * @param interpreter Interpreter instance we are serving
     */
    public final void setInterpreter(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    /**
     * Get the object we are putting
     *
     * @return the object we are putting
     */
    public Object getObject() {
        return object;
    }

    /**
     * Set the object we are putting
     *
     * @param object the object we are putting
     */
    public final void setObject(Object object) {
        this.object = object;
    }

    /**
     * ctor/0
     */
    protected Putter() {
    }

    /**
     * Create instance to put an object via an the Interpreter we are servicing
     *
     * @param object object to put
     * @param interpreter Interpreter instance we are servicing
     */
    public Putter(Object object, Interpreter interpreter) {
        setObject(object);
        setInterpreter(interpreter);
        setCharsetName("ASCII");
    }

    /**
     * Create instance to put an object via an the Interpreter we are servicing
     * with a specified charset
     *
     * @param object object to put
     * @param interpreter Interpreter instance we are servicing
     * @param charsetName charset to use
     */
    public Putter(Object object, Interpreter interpreter, String charsetName) {
        this(object, interpreter);
        setCharsetName(charsetName);
    }

    /**
     * Put our object to the destination
     *
     * @param destDataSink destination
     * @throws SQLException
     * @throws IOException
     * @throws AS400SecurityException
     * @throws ErrorCompletingRequestException
     * @throws InterruptedException
     * @throws ObjectDoesNotExistException
     * @throws RequestNotSupportedException
     */
    public void put(DataSink destDataSink) throws SQLException, IOException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException, ObjectDoesNotExistException, RequestNotSupportedException {
        put(destDataSink, true);
    }

    /**
     * Put our object to the destination with an optional newline if to STD
     *
     * @param destDataSink destination
     * @param newline true if newline should be appended, false if space
     * appended
     * @throws SQLException
     * @throws IOException
     * @throws AS400SecurityException
     * @throws ErrorCompletingRequestException
     * @throws InterruptedException
     * @throws ObjectDoesNotExistException
     * @throws RequestNotSupportedException
     */
    public void put(DataSink destDataSink, boolean newline) throws SQLException, IOException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException, ObjectDoesNotExistException, RequestNotSupportedException {
        put(destDataSink, false, newline);
    }

    /**
     * Put our object to the destination with an optional newline and/or
     * optional trailing blank space if to STD
     *
     * @param destDataSink destination
     * @param space true if add a blank space to end of string
     * @param newline true if newline should be appended, false if space
     * appended
     * @throws SQLException
     * @throws IOException
     * @throws AS400SecurityException
     * @throws ErrorCompletingRequestException
     * @throws InterruptedException
     * @throws ObjectDoesNotExistException
     * @throws RequestNotSupportedException
     */
    public void put(DataSink destDataSink, boolean space, boolean newline) throws SQLException, IOException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException, ObjectDoesNotExistException, RequestNotSupportedException {
        put(destDataSink, false, space, newline);
    }

    /**
     * Put our object to the destination with an optional newline and/or
     * optional trailing blank space if to STD
     *
     * @param destDataSink destination
     * @param append if datasink is a file append data
     * @param space true if add a blank space to end of string
     * @param newline true if newline should be appended, false if space
     * appended
     * @throws SQLException
     * @throws IOException
     * @throws AS400SecurityException
     * @throws ErrorCompletingRequestException
     * @throws InterruptedException
     * @throws ObjectDoesNotExistException
     * @throws RequestNotSupportedException
     */
    public void put(DataSink destDataSink, boolean append, boolean space, boolean newline) throws SQLException, IOException, AS400SecurityException, ErrorCompletingRequestException, InterruptedException, ObjectDoesNotExistException, RequestNotSupportedException {
        String stringdata;
        switch (destDataSink.getType()) {
            case FILE:
                stringdata = new Renderer(getObject(), getCharsetName()).asString();
                File file = new File(destDataSink.getName());
                FileWriter fWriter = new FileWriter(file, append);
                BufferedWriter bWriter = new BufferedWriter(fWriter);
                bWriter.write(stringdata);
                bWriter.close();
                fWriter.close();
                break;
            case TUPLE:
                Tuple t = getInterpreter().getTuple(destDataSink.getName());
                getInterpreter().dbug().dbugTuple("sought Tuple in put case TUPLE: ", t);
                if (append) {
                    if (getObject() instanceof String) {
                        if (!(t == null)) {
                            Object o = t.getValue();
                            if (o instanceof String) {
                                setObject(t.getValue() + (space ? " " : "") + getObject().toString());
                            }
                        }
                    }
                }
                if (t == null) {
                    getInterpreter().setTuple(destDataSink.getName(), getObject());
                } else {
                    t.setValue(getObject());
                }
                break;
            case ERR:
                stringdata = new Renderer(getObject(), getCharsetName()).asString();
                if (space) {
                    stringdata = stringdata + " ";
                }
                if (newline) {
                    getInterpreter().getErroutStream().println(stringdata);
                } else {
                    getInterpreter().getErroutStream().print(stringdata);
                }
                break;
            case STD:
            default:
                stringdata = new Renderer(getObject(), getCharsetName()).asString();
                if (space) {
                    stringdata = stringdata + " ";
                }
                if (newline) {
                    getInterpreter().getOutputStream().println(stringdata);
                } else {
                    getInterpreter().getOutputStream().print(stringdata);
                }
                break;
            case LIFO:
                if (getObject() == null) {
                    getInterpreter().getTupleStack().push(new Tuple(null, null));
                } else {
                    if (getObject().getClass().equals(ublu.util.Tuple.class)) {
                        getInterpreter().getTupleStack().push(ublu.util.Tuple.class.cast(getObject()));
                    } else {
                        getInterpreter().getTupleStack().push(new Tuple(null, getObject()));
                    }
                }
                break;
            case NUL:
                // throw it away!
                break;
        }
    }
}
