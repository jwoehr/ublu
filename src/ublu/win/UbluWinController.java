/*
 * Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com
 * Copyright (c) 2017, Jack J. Woehr jwoehr@softwoehr.com 
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
package ublu.win;

import java.awt.Color;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import ublu.Ublu;
import ublu.command.CommandInterface.COMMANDRESULT;
import ublu.util.Generics;
import ublu.util.Generics.StringArrayList;
import ublu.util.Interpreter;
import ublu.util.Parser;

/**
 *
 * @author jax
 */
public class UbluWinController {

    /**
     *
     */
    public static final String PRODUCT_NAME = java.util.ResourceBundle.getBundle("ublu/win/Bundle").getString("productName");

    /**
     *
     */
    public final void aboutUblu() {
        JOptionPane.showMessageDialog(ubluFrame, Ublu.startupMessage(), PRODUCT_NAME, JOptionPane.PLAIN_MESSAGE, ubluIcon);
    }

    /**
     *
     */
    protected UbluFrame ubluFrame;

    /**
     *
     */
    protected Interpreter interpreter;

    /**
     *
     */
    protected Ublu ublu;

    /**
     *
     */
    protected TextAreaOutputStream ubluTAOS;

    /**
     *
     */
    protected File fileSaveSession;

    /**
     *
     */
    protected File lastOpened;

    /**
     *
     */
    protected File lastIncluded;

    /**
     *
     */
    protected File lastSavedSettings;

    /**
     *
     */
    protected ImageIcon ubluIcon;

    /**
     *
     */
    protected WinProps myWinProps;

    /**
     *
     * @return
     */
    public File getSaveSessionFile() {
        return fileSaveSession;
    }

    /**
     *
     * @param saveSessionFile
     */
    public void setSaveSessionFile(File saveSessionFile) {
        this.fileSaveSession = saveSessionFile;
    }

    /**
     *
     * @return
     */
    public TextAreaOutputStream getUbluTAOS() {
        return ubluTAOS;
    }

    /**
     *
     * @param ubluTAOS
     */
    public void setUbluTAOS(TextAreaOutputStream ubluTAOS) {
        this.ubluTAOS = ubluTAOS;
    }

    /**
     *
     */
    protected UbluWinInputStream ubluIS;

    /**
     *
     * @return
     */
    public Ublu getUblu() {
        return ublu;
    }

    /**
     *
     * @return
     */
    public UbluFrame getUbluFrame() {
        return ubluFrame;
    }

    /**
     *
     * @return
     */
    public Interpreter getInterpreter() {
        return interpreter;
    }

    /**
     *
     * @return
     */
    protected UbluWinInputStream getUbluIS() {
        return ubluIS;
    }

    /**
     *
     */
    public UbluWinController() {
        myWinProps = new WinProps(this);
        ubluIcon = createImageIcon("/ublu/resource/Candlespace.gif", "NASA candle in space");
    }

    /**
     *
     * @param interpreter
     */
    public UbluWinController(Interpreter interpreter) {
        this();
        this.interpreter = interpreter;
    }

    /**
     *
     * @param ublu
     */
    public UbluWinController(Ublu ublu) {
        this();
        this.ublu = ublu;
        this.interpreter = Ublu.getMainInterpreter();
    }

    /**
     *
     * @param input
     * @return
     */
    protected COMMANDRESULT interpretText(String input) {
        COMMANDRESULT result;
        input = input.trim();
        interpreter.setArgArray(new Parser(interpreter, input).parseAnArgArray());
        result = interpreter.loop();
        interpreter.prompt();
        return result;
    }

    /**
     *
     */
    public void startup() {
        ubluFrame = new UbluFrame();
        ubluFrame.setUbluWinController(this);
        ubluTAOS = ubluFrame.getUbluPanel().getjTAOS();
        ubluIS = new UbluWinInputStream();
        interpreter.setInputStream(ubluIS);
        interpreter.setInputStreamBufferedReader(new BufferedReader(new InputStreamReader(interpreter.getInputStream())));
        interpreter.setOutputStream(new PrintStream(ubluTAOS));
        interpreter.setErroutStream(new PrintStream(ubluTAOS));
        ublu.reinitLogger(new PrintStream(ubluTAOS));
        try {
            getPropsFromGetArgs();
        } catch (IOException ex) {
            Logger.getLogger(UbluWinController.class.getName()).log(Level.SEVERE, null, ex);
        }
        ubluFrame.runMe();
        ubluFrame.getUbluPanel().getUbluTextField().requestFocusInWindow();
        aboutUblu();
    }

    /**
     *
     * @return
     */
    protected File dialogForSaveFile() {
        File result = null;
        final JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (fileSaveSession != null) {
            fc.setSelectedFile(fileSaveSession);
        }
        fc.setMultiSelectionEnabled(false);
        int returnVal = fc.showDialog(ubluFrame, "Save Session As");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            result = fc.getSelectedFile();
        }
        return result;
    }

    /**
     *
     * @param f
     * @param s
     * @return
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    protected boolean saveToFile(File f, String s) throws FileNotFoundException, IOException {
        boolean result = true;
        if (!f.exists()) {
            if (f.createNewFile()) {
                writeToFile(f, s);
            } else {
                result = false;
            }
        } else {
            writeToFile(f, s);
        }
        return result;
    }

    private void writeToFile(File f, String s) throws FileNotFoundException, IOException {
        try (FileOutputStream fileOutputStream = new FileOutputStream(f)) {
            if (s == null) {
                JOptionPane.showMessageDialog(null, "Null text -- nothing saved");
            } else {
                fileOutputStream.write(s.getBytes());
                fileOutputStream.close();
            }
        }
    }

    /**
     *
     * @return @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    public boolean saveSession() throws FileNotFoundException, IOException {
        boolean result;
        String s = getUbluFrame().getUbluTextArea().getText();
        if (fileSaveSession != null) {
            result = saveToFile(fileSaveSession, s);
        } else {
            result = saveSessionAs();
        }
        return result;

    }

    /**
     *
     * @return @throws FileNotFoundException
     * @throws IOException
     */
    public boolean saveSessionAs() throws FileNotFoundException, IOException {
        boolean result = false;
        String s = getUbluFrame().getUbluTextArea().getText();
        File f = dialogForSaveFile();
        if (f != null) {
            fileSaveSession = f;
            if (f.exists()) {
                switch (confirmOverwrite()) {
                    case JOptionPane.YES_OPTION:
                        result = saveToFile(fileSaveSession, s);
                        break;
                    default:
                        JOptionPane.showMessageDialog(null, "Session not saved");
                }
            } else {
                result = saveToFile(fileSaveSession, s);
            }
        }
        return result;
    }

    /**
     *
     * @return @throws FileNotFoundException
     * @throws IOException
     */
    public boolean saveSelectedAs() throws FileNotFoundException, IOException {
        boolean result = false;
        String s = getUbluFrame().getUbluTextArea().getSelectedText();
        File f = dialogForSaveFile();
        if (f != null) {
            if (f.exists()) {
                switch (confirmOverwrite()) {
                    case JOptionPane.YES_OPTION:
                        result = saveToFile(f, s);
                        break;
                    default:
                        JOptionPane.showMessageDialog(null, "Selection not saved");
                }
            } else {
                result = saveToFile(f, s);
            }
        }
        return result;
    }

    private int confirmOverwrite() {
        int response = JOptionPane.showConfirmDialog(null, "File exists, overwrite?", "Confirm overwrite extant file",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return response;

    }

    /**
     *
     * @return
     */
    protected File dialogForLoadFile() {
        File result = null;
        final JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (lastOpened != null) {
            fc.setSelectedFile(lastOpened);
        }
        fc.setMultiSelectionEnabled(false);
        int returnVal = fc.showDialog(ubluFrame, "Open File");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            result = fc.getSelectedFile();
        }
        return result;
    }

    /**
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    protected void loadFile() throws FileNotFoundException, IOException {
        File f = dialogForLoadFile();
        if (f != null) {
            lastOpened = f;
            for (String s : new Generics.StringArrayList(Files.readAllLines(lastOpened.toPath()))) {
                getUbluFrame().getUbluTextArea().append(s + "\n");
            }
            getUbluFrame().getUbluPanel().scrollToEnd();
        }
    }

    /**
     *
     * @return
     */
    protected File dialogForIncludeFile() {
        File result = null;
        final JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (lastIncluded != null) {
            fc.setSelectedFile(lastIncluded);
        }
        fc.setMultiSelectionEnabled(false);
        int returnVal = fc.showDialog(ubluFrame, "Open File");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            result = fc.getSelectedFile();
        }
        return result;
    }

    /**
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    protected void includeFile() throws FileNotFoundException, IOException {
        File f = dialogForLoadFile();
        if (f != null) {
            lastIncluded = f;
            interpreter.include(FileSystems.getDefault().getPath(f.getAbsolutePath()));
            getUbluFrame().getUbluPanel().scrollToEnd();
        }
    }

    /**
     *
     * @return
     */
    protected File dialogForSaveSettings() {
        File result = null;
        final JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (lastSavedSettings != null) {
            fc.setSelectedFile(lastSavedSettings);
        }
        fc.setMultiSelectionEnabled(false);
        int returnVal = fc.showDialog(ubluFrame, "Save Settings");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            result = fc.getSelectedFile();
        }
        return result;
    }

    /**
     *
     * @return @throws FileNotFoundException
     * @throws IOException
     */
    public boolean saveSettingsAs() throws FileNotFoundException, IOException {
        boolean result = false;
        File f = dialogForSaveSettings();
        if (f != null) {
            if (f.exists()) {
                switch (confirmOverwrite()) {
                    case JOptionPane.YES_OPTION:
                        myWinProps.writeWindowingProperties(f.getAbsolutePath());
                        lastSavedSettings = f;
                        break;
                    default:
                        JOptionPane.showMessageDialog(null, "Selection not saved");
                }
            } else {
                myWinProps.writeWindowingProperties(f.getAbsolutePath());
                lastSavedSettings = f;
            }
        }
        return result;
    }

    /**
     *
     * @return
     */
    protected File dialogForLoadSettings() {
        File result = null;
        final JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (lastSavedSettings != null) {
            fc.setSelectedFile(lastSavedSettings);
        }
        fc.setMultiSelectionEnabled(false);
        int returnVal = fc.showDialog(ubluFrame, "Load Settings");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            result = fc.getSelectedFile();
        }
        return result;
    }

    /**
     *
     */
    public void restoreSettingsFromProps() {
        Integer ubluTextAreaFontStyle = Integer.parseInt(myWinProps.get("UbluTextAreaFontStyle", "0"));
        Integer ubluInputAreaFontStyle = Integer.parseInt(myWinProps.get("UbluInputAreaFontStyle", "0"));
        String ubluTextAreaFont = myWinProps.get("UbluTextAreaFont", "Lucida Sans Typewriter");
        String ubluInputAreaFont = myWinProps.get("UbluInputAreaFont", "Lucida Sans Typewriter");
        Integer ubluTextAreaFontSize = Integer.parseInt(myWinProps.get("UbluTextAreaFontSize", "13"));
        Integer ubluInputAreaFontSize = Integer.parseInt(myWinProps.get("UbluInputAreaFontSize", "13"));
        Integer ubluTextAreaFGColor = (int) Long.parseLong(myWinProps.get("UbluTextAreaFGColor", "ff333333"), 16);
        Integer ubluInputAreaFGColor = (int) Long.parseLong(myWinProps.get("UbluInputAreaFGColor", "ff333333"), 16);
        Integer ubluTextAreaBGColor = (int) Long.parseLong(myWinProps.get("UbluTextAreaBGColor", "ffffff"), 16);
        Integer ubluInputAreaBGColor = (int) Long.parseLong(myWinProps.get("UbluInputAreaBGColor", "ffffff"), 16);

        ubluFrame.getUbluTextArea().setFont(new Font(ubluTextAreaFont, ubluTextAreaFontStyle, ubluTextAreaFontSize));
        ubluFrame.getUbluInputArea().setFont(new Font(ubluInputAreaFont, ubluInputAreaFontStyle, ubluInputAreaFontSize));
        ubluFrame.getUbluTextArea().setForeground(new Color(ubluTextAreaFGColor));
        ubluFrame.getUbluInputArea().setForeground(new Color(ubluInputAreaFGColor));
        ubluFrame.getUbluTextArea().setCaretColor(new Color(ubluTextAreaFGColor));
        ubluFrame.getUbluInputArea().setCaretColor(new Color(ubluInputAreaFGColor));
        ubluFrame.getUbluTextArea().setBackground(new Color(ubluTextAreaBGColor));
        ubluFrame.getUbluInputArea().setBackground(new Color(ubluInputAreaBGColor));
        ubluFrame.revalidate();
    }

    /**
     *
     * @throws IOException
     */
    public void getPropsFromGetArgs() throws IOException {
        StringArrayList sal = ublu.getMyGetArgs().getAllIdenticalOptionArguments("-w");
        if (!sal.isEmpty()) {
            String filepath = sal.get(0);
            if (filepath != null) {
                myWinProps.readIn(filepath);
                restoreSettingsFromProps();
                lastSavedSettings = new File(filepath);
            }
        }
    }

    /**
     *
     * @return @throws FileNotFoundException
     * @throws IOException
     */
    public boolean loadSettingsAs() throws FileNotFoundException, IOException {
        boolean result = false;
        File f = dialogForLoadSettings();
        if (f != null) {
            myWinProps.readIn(f.getAbsolutePath());
            restoreSettingsFromProps();
            result = true;
        }
        return result;
    }

    /**
     * Returns an ImageIcon, or null if the path was invalid.
     *
     * @param path
     * @param description
     * @return
     */
    protected final ImageIcon createImageIcon(String path,
            String description) {
        java.net.URL imgURL = getClass().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    /**
     *
     */
    protected void help() {
        JOptionPane.showMessageDialog(null, windowingHelp(), "Ublu Windowing Help", JOptionPane.PLAIN_MESSAGE, ubluIcon);
    }

    /**
     *
     * @return
     */
    protected String windowingHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append("Welcome to windowing Ublu.\n\n")
                .append("Information about Ublu itself is found in two documents:\n\n")
                .append("   * The Ublu Reference (userdoc/ubluref.html)\n")
                .append("   * The Ublu Guide (userdoc/ubluguide.html)\n\n")
                .append("This informational dialog is about windowing Ublu.\n\n")
                .append("The top larger area is the Ublu text area. Ublu output appears in the text area.\n")
                .append("You can edit that area, save it, save selections, or cause Ublu to interpret selections.\n")
                .append("See the File menu and Ublu menu options.\n\n")
                .append("The bottom area is the Ublu input line. Hitting Enter there causes input text to be interpreted.\n")
                .append("You can edit the input line or move through previous input lines with the up and down arrow keys.\n")
                .append("Multi-line input such as extended blocks or strings does not work in the input line. Instead, type one or more lines\n")
                .append("of input into the upper text area, select those lines with keys or the mouse, and choose Ublu->Interpret Selected.\n\n")
                .append("Ctrl-TAB toggles between the Ublu input line and the Ublu text area.\n\n")
                .append("The Settings menu allows you change settings and save them to a properties file.\n")
                .append("You can later load these settings from the Settings menu or by providing the properties file path\n")
                .append("to Ublu as an argument to the -w switch which launches Ublu windowing.\n\n")
                .append("You may wish to set:\n\n")
                .append("   props -set signon.handler.type BUILTIN\n\n")
                .append("so that on a password or username error Ublu prompts graphically instead of to the console.");

        return sb.toString();
    }
}
