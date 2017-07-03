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
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import ublu.command.CommandInterface;

/**
 *
 * @author jax
 */
public class UbluFrame extends javax.swing.JFrame {

    private static UbluFrame SINGLETON;

    private UbluWinController ubluWinController;

    /**
     *
     * @return
     */
    protected UbluWinController getUbluWinController() {
        return ubluWinController;
    }

    /**
     *
     * @param ubluWinController
     */
    protected void setUbluWinController(UbluWinController ubluWinController) {
        this.ubluWinController = ubluWinController;
    }

    /**
     *
     * @return
     */
    protected UbluPanel getUbluPanel() {
        return ubluPanel;
    }

    /**
     *
     * @return
     */
    protected JTextArea getUbluTextArea() {
        return getUbluPanel().getUbluTextArea();
    }

    /**
     *
     * @return
     */
    protected JTextField getUbluInputArea() {
        return getUbluPanel().getUbluTextField();
    }

    /**
     *
     * @return
     */
    protected UbluWinInputStream getUbluWinInputStream() {
        return getUbluWinController().getUbluIS();
    }

    /**
     *
     * @param input
     * @return
     */
    protected CommandInterface.COMMANDRESULT interpretText(String input) {
        return getUbluWinController().interpretText(input);
    }

    /**
     * Creates new form UbluFrame
     */
    public UbluFrame() {
        initComponents();
    }

    /**
     *
     */
    protected void runMe() {
        ubluPanel.setUbluFrame(this);
        SINGLETON = this;
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(UbluFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                SINGLETON.setVisible(true);
            }
        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        ubluPanel = new ublu.win.UbluPanel();
        ubluMenuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        OpenMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        saveMenuItem = new javax.swing.JMenuItem();
        saveAsMenuItem = new javax.swing.JMenuItem();
        saveSelectedAsMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        quitMenuItem = new javax.swing.JMenuItem();
        ubluMenu = new javax.swing.JMenu();
        includeMenuItem = new javax.swing.JMenuItem();
        ubluSelectedMenuItem = new javax.swing.JMenuItem();
        settingsMenu = new javax.swing.JMenu();
        fontMenuItem = new javax.swing.JMenuItem();
        backgroundMenuItem = new javax.swing.JMenuItem();
        saveSettingsMenuItem = new javax.swing.JMenuItem();
        loadSettingjMenuItem = new javax.swing.JMenuItem();
        defaultsMenuItem = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        aboutMenuItem = new javax.swing.JMenuItem();
        helpMenuItem = new javax.swing.JMenuItem();

        jMenuItem1.setText("jMenuItem1");

        jMenuItem2.setText("jMenuItem2");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("ublu/win/Bundle"); // NOI18N
        setTitle(bundle.getString("productName")); // NOI18N

        fileMenu.setText("File");
        fileMenu.setToolTipText("File operations in the text area and exiting the application");

        OpenMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        OpenMenuItem.setText("Open");
        OpenMenuItem.setToolTipText("Read a file into the text area");
        OpenMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                OpenMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(OpenMenuItem);
        fileMenu.add(jSeparator2);

        saveMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        saveMenuItem.setText("Save");
        saveMenuItem.setToolTipText("Save the output to last chosen output save file");
        saveMenuItem.setActionCommand("SaveMenuItem");
        saveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveMenuItem);

        saveAsMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.CTRL_MASK));
        saveAsMenuItem.setText("Save As");
        saveAsMenuItem.setToolTipText("Save output to specific file");
        saveAsMenuItem.setActionCommand("SaveAsMenuItem");
        saveAsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveAsMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveAsMenuItem);

        saveSelectedAsMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.CTRL_MASK));
        saveSelectedAsMenuItem.setToolTipText("Save selected text to a file");
        saveSelectedAsMenuItem.setActionCommand("SaveSelectedAs");
        saveSelectedAsMenuItem.setLabel("Save Selected As");
        saveSelectedAsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveSelectedAsMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveSelectedAsMenuItem);
        fileMenu.add(jSeparator1);

        quitMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_MASK));
        quitMenuItem.setText("Quit");
        quitMenuItem.setToolTipText("Tell Ublu bye to exit the application");
        quitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                quitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(quitMenuItem);

        ubluMenuBar.add(fileMenu);

        ubluMenu.setToolTipText("Ublu interpreting and including");
        ubluMenu.setActionCommand("ubluMenu");
        ubluMenu.setLabel("Ublu");

        includeMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.CTRL_MASK));
        includeMenuItem.setText("Include");
        includeMenuItem.setToolTipText("Include an Ublu source file");
        includeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                includeMenuItemActionPerformed(evt);
            }
        });
        ubluMenu.add(includeMenuItem);

        ubluSelectedMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, java.awt.event.InputEvent.CTRL_MASK));
        ubluSelectedMenuItem.setText("Interpret Selected");
        ubluSelectedMenuItem.setToolTipText("Interpret selected text in text area");
        ubluSelectedMenuItem.setActionCommand("InterpretSelected");
        ubluSelectedMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ubluSelectedMenuItemActionPerformed(evt);
            }
        });
        ubluMenu.add(ubluSelectedMenuItem);

        ubluMenuBar.add(ubluMenu);

        settingsMenu.setText("Settings");

        fontMenuItem.setText("Font");
        fontMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fontMenuItemActionPerformed(evt);
            }
        });
        settingsMenu.add(fontMenuItem);

        backgroundMenuItem.setText("Background");
        backgroundMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backgroundMenuItemActionPerformed(evt);
            }
        });
        settingsMenu.add(backgroundMenuItem);

        saveSettingsMenuItem.setText("Save Settings");
        saveSettingsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveSettingsMenuItemActionPerformed(evt);
            }
        });
        settingsMenu.add(saveSettingsMenuItem);

        loadSettingjMenuItem.setText("Load Settings");
        loadSettingjMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadSettingsMenuItemActionPerformed(evt);
            }
        });
        settingsMenu.add(loadSettingjMenuItem);

        defaultsMenuItem.setText("Restore Defaults");
        defaultsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                defaultsMenuItemActionPerformed(evt);
            }
        });
        settingsMenu.add(defaultsMenuItem);

        ubluMenuBar.add(settingsMenu);

        helpMenu.setText("Help");
        helpMenu.setToolTipText("Help and info");

        aboutMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_B, java.awt.event.InputEvent.CTRL_MASK));
        aboutMenuItem.setText("About");
        aboutMenuItem.setToolTipText("About Ublu");
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(aboutMenuItem);

        helpMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.CTRL_MASK));
        helpMenuItem.setText("Help");
        helpMenuItem.setToolTipText("Display Ublu windowing help");
        helpMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(helpMenuItem);

        ubluMenuBar.add(helpMenu);

        setJMenuBar(ubluMenuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(ubluPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 1056, Short.MAX_VALUE)
                .addGap(0, 0, 0))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(ubluPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 610, Short.MAX_VALUE)
                .addGap(0, 0, 0))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void saveAsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveAsMenuItemActionPerformed
        try {
            ubluWinController.saveSessionAs();
        } catch (IOException ex) {
            Logger.getLogger(UbluFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_saveAsMenuItemActionPerformed

    private void ubluSelectedMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ubluSelectedMenuItemActionPerformed
        String s = ubluPanel.getUbluTextArea().getSelectedText();
        if (s != null) {
            ubluPanel.scrollToEnd();
            interpretText(s);
            ubluPanel.scrollToEnd();
        }
    }//GEN-LAST:event_ubluSelectedMenuItemActionPerformed

    private void saveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMenuItemActionPerformed
        try {
            ubluWinController.saveSession();
        } catch (IOException ex) {
            Logger.getLogger(UbluFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_saveMenuItemActionPerformed

    private void saveSelectedAsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveSelectedAsMenuItemActionPerformed
        try {
            ubluWinController.saveSelectedAs();
        } catch (IOException ex) {
            Logger.getLogger(UbluFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_saveSelectedAsMenuItemActionPerformed

    private void OpenMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_OpenMenuItemActionPerformed
        try {
            ubluWinController.loadFile();
        } catch (IOException ex) {
            Logger.getLogger(UbluFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_OpenMenuItemActionPerformed

    private void includeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_includeMenuItemActionPerformed
        try {
            ubluWinController.includeFile();
        } catch (IOException ex) {
            Logger.getLogger(UbluFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_includeMenuItemActionPerformed

    private void helpMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpMenuItemActionPerformed
        ubluWinController.help();
    }//GEN-LAST:event_helpMenuItemActionPerformed

    private void quitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_quitMenuItemActionPerformed
        ubluWinController.interpretText("bye\n");
    }//GEN-LAST:event_quitMenuItemActionPerformed

    private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
        ubluWinController.aboutUblu();
    }//GEN-LAST:event_aboutMenuItemActionPerformed

    private void fontMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fontMenuItemActionPerformed
        FontChooser fc = new FontChooser(this, getUbluInputArea().getFont(), getUbluInputArea().getForeground());
        fc.setVisible(true);

        Font font = fc.getNewFont();
        if (font != null) {
            getUbluTextArea().setFont(font);
            getUbluInputArea().setFont(font);
            ubluWinController.myWinProps.set("UbluTextAreaFont", getUbluTextArea().getFont().getName());
            ubluWinController.myWinProps.set("UbluInputAreaFont", getUbluInputArea().getFont().getName());
            ubluWinController.myWinProps.set("UbluTextAreaFontStyle", Integer.toString(getUbluTextArea().getFont().getStyle()));
            ubluWinController.myWinProps.set("UbluInputAreaFontStyle", Long.toString(getUbluInputArea().getFont().getStyle()));
            ubluWinController.myWinProps.set("UbluTextAreaFontSize", Long.toString(getUbluTextArea().getFont().getSize()));
            ubluWinController.myWinProps.set("UbluInputAreaFontSize", Long.toString(getUbluInputArea().getFont().getSize()));
        }
        Color color = fc.getNewColor();
        if (color != null) {
            getUbluTextArea().setForeground(color);
            ubluWinController.myWinProps.set("UbluTextAreaFGColor", Integer.toHexString(color.getRGB()));
            getUbluInputArea().setForeground(color);
            ubluWinController.myWinProps.set("UbluInputAreaFGColor", Integer.toHexString(color.getRGB()));
        }
        revalidate();
    }//GEN-LAST:event_fontMenuItemActionPerformed

    private void saveSettingsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveSettingsMenuItemActionPerformed
        try {
            ubluWinController.saveSettingsAs();
        } catch (IOException ex) {
            Logger.getLogger(UbluFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_saveSettingsMenuItemActionPerformed

    private void loadSettingsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadSettingsMenuItemActionPerformed
        try {
            ubluWinController.loadSettingsAs();
        } catch (IOException ex) {
            Logger.getLogger(UbluFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        revalidate();
    }//GEN-LAST:event_loadSettingsMenuItemActionPerformed

    private void backgroundMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backgroundMenuItemActionPerformed
        ColorChooser cc = new ColorChooser(this, true);
        cc.setVisible(true);

        if (cc.ok) {
            Color color = cc.getColor();
            getUbluTextArea().setBackground(color);
            ubluWinController.myWinProps.set("UbluTextAreaBGColor", Integer.toHexString(color.getRGB()));
            getUbluInputArea().setBackground(color);
            ubluWinController.myWinProps.set("UbluInputAreaBGColor", Integer.toHexString(color.getRGB()));
        }
        revalidate();
    }//GEN-LAST:event_backgroundMenuItemActionPerformed

    private void defaultsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_defaultsMenuItemActionPerformed
        ubluWinController.myWinProps.setDefaultWindowingProperties();
        ubluWinController.restoreSettingsFromProps();
        revalidate();
    }//GEN-LAST:event_defaultsMenuItemActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(UbluFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(UbluFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(UbluFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(UbluFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new UbluFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem OpenMenuItem;
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JMenuItem backgroundMenuItem;
    private javax.swing.JMenuItem defaultsMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenuItem fontMenuItem;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem helpMenuItem;
    private javax.swing.JMenuItem includeMenuItem;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JMenuItem loadSettingjMenuItem;
    private javax.swing.JMenuItem quitMenuItem;
    private javax.swing.JMenuItem saveAsMenuItem;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JMenuItem saveSelectedAsMenuItem;
    private javax.swing.JMenuItem saveSettingsMenuItem;
    private javax.swing.JMenu settingsMenu;
    private javax.swing.JMenu ubluMenu;
    private javax.swing.JMenuBar ubluMenuBar;
    private ublu.win.UbluPanel ubluPanel;
    private javax.swing.JMenuItem ubluSelectedMenuItem;
    // End of variables declaration//GEN-END:variables
}
