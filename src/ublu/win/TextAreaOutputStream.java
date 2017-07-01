/*
 * JTextAreaOutputStream.java
 *
 * Created on October 13, 2000, 10:58 PM
 * Copyright (C) 2000, 2016 Jack J. Woehr
 * jwoehr@softwoehr.com PO Box 51, Golden, Colorado 80402 USA
 */
package ublu.win;

import javax.swing.JTextArea;

/**
 * Create an OutputStream that puts its contents to a JTextArea
 *
 * @author jax
 */
public class TextAreaOutputStream extends java.io.OutputStream {

    private JTextArea my_text_area;
    private UbluPanel myUbluPanel = null;

    /**
     * Create without instancing text area
     */
    public TextAreaOutputStream() {
    }

    /**
     * Create while instancing text area.
     *
     * @param jta The JTextArea for this stream to use.
     */
    public TextAreaOutputStream(JTextArea jta) {
        set_text_area(jta);
    }

    /**
     *
     * @param ubluPanel
     */
    public TextAreaOutputStream(UbluPanel ubluPanel) {
        this(ubluPanel.getUbluTextArea());
        myUbluPanel = ubluPanel;
    }

    /**
     * Set the text area for this output.
     *
     * @param jta The JTextArea for this stream to use.
     */
    public final void set_text_area(JTextArea jta) {
        my_text_area = jta;
    }

    /**
     * Get the text area for this output.
     *
     * @return The JTextArea used for output from this stream.
     */
    public JTextArea get_text_area() {
        return my_text_area;
    }

    /**
     * Error message used by write()
     */
    private String no_text_area_message() {
        return this + " doesn't have an associated JTextArea.";
    }

    class NullTextAreaException extends Exception {

        NullTextAreaException(String s) {
            super(s);
        }
    }

    /**
     * Write a byte to the output stream.
     *
     * @param b Byte to write.
     */
    @Override
    public void write(int b) {
        byte b1[] = new byte[1];
        b1[0] = (byte) b;
        String s = new String(b1);

        /**
         * Append if text area, otherwise print error.
         */
        if (null != get_text_area()) {
            my_text_area.append(s);
            if (myUbluPanel != null) {
                myUbluPanel.scrollToEnd();
            }
        } else {
            System.err.println(no_text_area_message());
        }
    }

    /**
     * Write a byte array to the output stream.
     *
     * @param b Byte array to write.
     */
    @Override
    public void write(byte b[]) {
        String s = new String(b);

        /**
         * Append if text area, otherwise print error.
         */
        if (null != get_text_area()) {
            my_text_area.append(s);
            if (myUbluPanel != null) {
                myUbluPanel.scrollToEnd();
            }
        } else {
            System.err.println(no_text_area_message());
        }
    }

    /**
     * Write a byte subarray to the output stream.
     *
     * @param b The byte array.
     * @param offset Offset to start at.
     * @param length Length to write.
     */
    @Override
    public void write(byte b[], int offset, int length) {
        String s = new String(b, offset, length);

        /**
         * Append if text area, otherwise print error.
         */
        if (null != get_text_area()) {
            my_text_area.append(s);
            if (myUbluPanel != null) {
                myUbluPanel.scrollToEnd();
            }
        } else {
            System.err.println(no_text_area_message());
        }
    }
}
