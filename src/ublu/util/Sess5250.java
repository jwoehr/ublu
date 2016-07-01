/*
 * Copyright (c) 2015, Absolute Performance, Inc. http://www.absolute-performance.com
 * Copyright (c) 2016, Jack J. Woehr jwoehr@softwoehr.com http://www.softwoehr.com
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

import java.awt.Point;
import org.tn5250j.Session5250;
import org.tn5250j.SessionPanel;
import org.tn5250j.framework.common.SessionManager;
import org.tn5250j.framework.tn5250.Screen5250;

/**
 * Wrapper for tn5250 session object(s)
 *
 * @author jwoehr
 */
public class Sess5250 {

    Session5250 sess;

    /**
     * Instance with Session5250 set
     *
     * @param sess Session5250 which this object wrappers
     */
    public Sess5250(Session5250 sess) {
        this.sess = sess;
    }

    /**
     * True IFF connected
     *
     * @return True IFF connected
     */
    public boolean isConnected() {
        return getSessionGui().isConnected();
    }

    /**
     * Disconnect the session
     */
    public void disconnect() {
        sess.disconnect();
    }

    /**
     * Close the session, disconnecting if necessary
     */
    public void close() {
        if (isConnected()) {
            disconnect();
        }
        getSessionGui().closeDown();
        sess.getSessionManager().closeSession(getSessionGui());
    }

    /**
     * Get the tn5250 Session Manager for tn5250 sessions
     *
     * @return the tn5250 Session Manager
     */
    public SessionManager getSessionManager() {
        return sess.getSessionManager();
    }

    /**
     * Get the Session->SessionPanel
     *
     * @return Session->SessionPanel
     */
    public SessionPanel getSessionGui() {
        return sess.getGUI();
    }

    /**
     * Get Session->SessionPanel->Screen
     *
     * @return Session->SessionPanel->Screen
     */
    public Screen5250 getScreen() {
        return getSessionGui().getScreen();
    }

    /**
     * Get the current cursor in the 5250 session as java Point
     *
     * @return the current cursor in the 5250 session as java Point
     */
    public Point getCursor() {
        Screen5250 scr = getScreen();
        int x = scr.getCol(scr.getCurrentPos());
        int y = scr.getRow(scr.getCurrentPos());
        Point p = new Point(x, y);
        return p;
    }

    /**
     * Set the desired cursor in the 5250 session
     *
     * @param p the desired cursor in the 5250 session as java Point
     * @return this
     */
    public Sess5250 setCursor(Point p) {
        Screen5250 scr = getScreen();
        scr.setCursor(p.y, p.x);
        return this;
    }

    /**
     * Send a string consisting of plain text and [control keys]
     *
     * @param s string consisting of plain text and [control keys]
     */
    public void sendKeys(String s) {
        getScreen().sendKeys(s);
    }

    /**
     * Return a formatted string of the current screen display
     *
     * @return a formatted string of the current screen display
     */
    public String screenDump() {
        // getScreen().dumpScreen();
        StringBuilder sb = new StringBuilder();
        char[] s = getScreen().getScreenAsChars();
        int c = getScreen().getColumns();
        int l = getScreen().getRows() * c;
        int col = 0;
        for (int x = 0; x < l; x++, col++) {
            sb.append(s[x]);
            if (col == c) {
                sb.append('\n');
                col = 0;
            }
        }
        return sb.toString();
    }
}
