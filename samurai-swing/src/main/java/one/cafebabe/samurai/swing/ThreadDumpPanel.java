/*
 * Copyright 2003-2012 Yusuke Yamamoto
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package one.cafebabe.samurai.swing;

import one.cafebabe.samurai.core.ThreadDumpExtractor;
import one.cafebabe.samurai.core.ThreadDumpSequence;
import one.cafebabe.samurai.core.ThreadStatistic;
import one.cafebabe.samurai.util.*;
import one.cafebabe.samurai.core.FullThreadDump;
import one.cafebabe.samurai.core.ThreadDump;
import one.cafebabe.samurai.web.Constants;
import one.cafebabe.samurai.web.ThreadFilter;
import one.cafebabe.samurai.web.VelocityHtmlRenderer;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ThreadDumpPanel extends LogRenderer implements HyperlinkListener,
        ConfigurationListener, ClipBoardOperationListener {
    public String config_dumpFontFamily = "Monospace";
    public String config_dumpFontSize = "12";
    private final Properties velocityContext = new Properties();

    private final JProgressBar progressBar = new JProgressBar();

    final ImageIcon forwardIcon = ImageLoader.get("/one/cafebabe/samurai/swing/images/forward.gif");
    final ImageIcon backwardIcon = ImageLoader.get("/one/cafebabe/samurai/swing/images/backward.gif");
    final public JButton saveButton = new JButton();
    public final JButton openButton = new JButton();
    public final JButton trashButton = new JButton();
    public final JToggleButton tableButton = new JToggleButton();
    public final JToggleButton fullButton = new JToggleButton();
    public final JToggleButton sequenceButton = new JToggleButton();
    final ImageIcon saveButtonIcon = ImageLoader.get("/one/cafebabe/samurai/swing/images/save.gif");
    ImageIcon openButtonIcon;
    final ImageIcon trashButtonIcon = ImageLoader.get("/one/cafebabe/samurai/swing/images/trash.gif");
    final ImageIcon tableIcon = ImageLoader.get("/one/cafebabe/samurai/swing/images/tableButton.gif");
    final ImageIcon fullButtonIcon = ImageLoader.get("/one/cafebabe/samurai/swing/images/fullButton.gif");
    final ImageIcon sequenceButtonIcon = ImageLoader.get("/one/cafebabe/samurai/swing/images/sequenceButton.gif");
    final BorderLayout borderLayout1 = new BorderLayout();
    private String referer = null;
    final JEditorPane threadDumpPanel = new JEditorPane() {
        public void paint(Graphics g) {
            super.paint(g);
            if (referer != null) {
                SwingUtilities.invokeLater(() -> {
                    threadDumpPanel.scrollToReference(referer);
                    referer = null;
                });
            }
        }
    };
    final JPanel settingPanel = new JPanel();
    private static final GUIResourceBundle resources = GUIResourceBundle.getInstance();
    private final VelocityHtmlRenderer renderer = new VelocityHtmlRenderer("one/cafebabe/samurai/swing/css.vm");

    public boolean config_wrapDump = false;

    private final Context context;

    public ThreadDumpPanel(SamuraiPanel samuraiPanel, Context context) {
        super(true, samuraiPanel);
        this.context = context;
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        resources.inject(this);
    }

    void jbInit() {
        this.setLayout(borderLayout1);
        this.setMaximumSize(new Dimension(2147483647, 2147483647));
        this.setMinimumSize(new Dimension(0, 0));
        this.setPreferredSize(new Dimension(400, 800));
        this.setLayout(borderLayout1);
        threadDumpPanel.setDoubleBuffered(true);
        threadDumpPanel.setEditable(false);
        threadDumpPanel.setText(resources.getMessage("ThreadDumpPanel.threadDumpHere"));
        threadDumpPanel.setContentType("text/html; charset=charset=UTF-8");
        settingPanel.setEnabled(true);
        settingPanel.setMaximumSize(new Dimension(2147483647, 40));
        settingPanel.setMinimumSize(new Dimension(10, 40));
        settingPanel.setPreferredSize(new Dimension(10, 40));
        settingPanel.setLayout(gridBagLayout1);

        buttonPrevious.setBorderPainted(false);
        buttonPrevious.setMaximumSize(new Dimension(20, 20));
        buttonPrevious.setMinimumSize(new Dimension(20, 20));
        buttonPrevious.setPreferredSize(new Dimension(20, 20));
        buttonPrevious.addMouseListener(new RolloverBorder(buttonPrevious));
        buttonPrevious.setIcon(backwardIcon);
        buttonPrevious.addActionListener(e -> {
            if (Constants.MODE_FULL.equals(filter.getMode())) {
                int selected = filter.getFullThreadIndex();
                if (selected > 0) {
                    selected--;
                    filter.setFullThreadIndex(selected);
                }
            } else if (Constants.MODE_SEQUENCE.equals(filter.getMode())) {
                ThreadDumpSequence[] sequences = statistic.getStackTracesAsArray();
                for (int i = 0; i < sequences.length; i++) {
                    if (sequences[i].getId().equals(filter.getThreadId())) {
                        if (i != 0) {
                            filter.setThreadId(sequences[i - 1].getId());
                        } else {
                            //looping
                            filter.setThreadId(sequences[sequences.length - 1].getId());
                        }
                        break;
                    }
                }
            }
            updateHtml();
        });


        buttonNext.setBorderPainted(false);
        buttonNext.setMaximumSize(new Dimension(20, 20));
        buttonNext.setMinimumSize(new Dimension(20, 20));
        buttonNext.setPreferredSize(new Dimension(20, 20));
        buttonNext.setIcon(forwardIcon);
        buttonNext.addMouseListener(new RolloverBorder(buttonNext));

        buttonNext.addActionListener(e -> {
            if (Constants.MODE_FULL.equals(filter.getMode())) {
                int selected = filter.getFullThreadIndex();
                if (selected < statistic.getFullThreadDumpCount() - 1) {
                    selected++;
                    filter.setFullThreadIndex(selected);
                }
            } else if (Constants.MODE_SEQUENCE.equals(filter.getMode())) {
                ThreadDumpSequence[] sequences = statistic.getStackTracesAsArray();
                for (int i = 0; i < sequences.length; i++) {
                    if (sequences[i].getId().equals(filter.getThreadId())) {
                        if (i != (sequences.length - 1)) {
                            filter.setThreadId(sequences[i + 1].getId());
                        } else {
                            //looping
                            filter.setThreadId(sequences[0].getId());
                        }
                        break;
                    }
                }
            }
            updateHtml();
        });


        jSplitPane1.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        jSplitPane1.setOneTouchExpandable(true);
        jSplitPane1.setMinimumSize(new Dimension(50, 50));
        jSplitPane1.setPreferredSize(new Dimension(50, 50));
        jSplitPane1.setContinuousLayout(true);
        jSplitPane1.setBorder(null);
        progressBar.setStringPainted(true);
        showThreadList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//    showThreadList.addMouseListener(new
//      FullThreadDumpPanel_showThreadList_mouseAdapter(this));
        showThreadList.addListSelectionListener(event -> {
//        ( (StackTraces) showThreadList.getSelectedValue()).getId().equals(filter.getThreadId())
//        showThreadList.getSelectedIndex()
            if (-1 != showThreadList.getSelectedIndex()) {
                if (Constants.MODE_FULL.equals(filter.getMode())) {
                    //ensure that selected thread comes to the top of the pane
//            JScrollBar scrollBar = jScrollPane3.getVerticalScrollBar();
//            scrollBar.setValue(scrollBar.getMaximum());
                    threadDumpPanel.scrollToReference(((ThreadDumpSequence) showThreadList.getSelectedValue()).getId());
//            context.getSearchPanel().searchNext(threadDumpPanel, ( (StackTraces) showThreadList.getSelectedValue()).getId(), 0);
//            threadDumpPanel.grabFocus();
                } else if (Constants.MODE_SEQUENCE.equals(filter.getMode())) {
                    if (!((ThreadDumpSequence) showThreadList.getSelectedValue()).getId().equals(
                            filter.getThreadId())) {
                        filter.setThreadId(((ThreadDumpSequence) showThreadList.getSelectedValue()).getId());
                        updateHtml();
                    }
                } else if (Constants.MODE_TABLE.equals(filter.getMode())) {
                    filter.setMode(Constants.MODE_SEQUENCE);
                    filter.setThreadId(((ThreadDumpSequence) showThreadList.getSelectedValue()).getId());
                    updateHtml();
                }
            }
        });
        showThreadList.setCellRenderer(new ThreadCellRenderer());


        saveButton.setBorderPainted(false);
        saveButton.setMaximumSize(new Dimension(20, 20));
        saveButton.setMinimumSize(new Dimension(20, 20));
        saveButton.setPreferredSize(new Dimension(20, 20));
        saveButton.setToolTipText("*ThreadDumpPanel.saveAsHtml*");
        saveButton.setFocusPainted(false);
        saveButton.setIcon(saveButtonIcon);
        saveButton.addActionListener(buttonListener);
        saveButton.addMouseListener(new RolloverBorder(saveButton));

        if (OSDetector.isMac()) {
            openButtonIcon = ImageLoader.get("/one/cafebabe/samurai/swing/images/folder_mac.gif");
        }
        if (OSDetector.isWindows()) {
            openButtonIcon = ImageLoader.get("/one/cafebabe/samurai/swing/images/folder_win.gif");
        }

        openButton.setBorderPainted(false);
        openButton.setMaximumSize(new Dimension(20, 20));
        openButton.setMinimumSize(new Dimension(20, 20));
        openButton.setPreferredSize(new Dimension(20, 20));
        openButton.setToolTipText("*ThreadDumpPanel.openFolder*");
        openButton.setFocusPainted(false);
        openButton.setIcon(openButtonIcon);
        openButton.addActionListener(buttonListener);
        openButton.setEnabled(false);
        openButton.addMouseListener(new RolloverBorder(openButton));


        trashButton.setBorderPainted(false);
        trashButton.setMaximumSize(new Dimension(20, 20));
        trashButton.setMinimumSize(new Dimension(20, 20));
        trashButton.setPreferredSize(new Dimension(20, 20));
        trashButton.setToolTipText("*ThreadDumpPanel.clear*");
        trashButton.setFocusPainted(false);
        trashButton.setIcon(trashButtonIcon);
        trashButton.addActionListener(buttonListener);
        trashButton.setEnabled(true);
        trashButton.addMouseListener(new RolloverBorder(trashButton));


        tableButton.setMaximumSize(new Dimension(20, 20));
        tableButton.setMinimumSize(new Dimension(20, 20));
        tableButton.setPreferredSize(new Dimension(20, 20));
        tableButton.setToolTipText("*ThreadDumpPanel.tableView*");
        tableButton.setFocusPainted(false);
        tableButton.setIcon(tableIcon);
        tableButton.addActionListener(buttonListener);
        tableButton.setSelected(true);

        sequenceButton.setMaximumSize(new Dimension(20, 20));
        sequenceButton.setMinimumSize(new Dimension(20, 20));
        sequenceButton.setPreferredSize(new Dimension(20, 20));
        sequenceButton.setToolTipText("*ThreadDumpPanel.sequenceView*");
        sequenceButton.setFocusPainted(false);
        sequenceButton.setIcon(sequenceButtonIcon);
        sequenceButton.addActionListener(buttonListener);

        fullButton.setMaximumSize(new Dimension(20, 20));
        fullButton.setMinimumSize(new Dimension(20, 20));
        fullButton.setPreferredSize(new Dimension(20, 20));
        fullButton.setToolTipText("*ThreadDumpPanel.threadDumpView*");
        fullButton.setFocusPainted(false);
        fullButton.setIcon(fullButtonIcon);
        fullButton.addActionListener(buttonListener);

        threadDumpPanel.addHyperlinkListener(this);
        threadDumpStatus.setText("");
        progressBar.setMaximumSize(new Dimension(80, 20));
        progressBar.setPreferredSize(new Dimension(80, 20));
        progressBar.setMinimumSize(new Dimension(80, 20));
        progressBar.setVisible(false);
        this.add(jSplitPane1, BorderLayout.CENTER);
        jSplitPane1.add(jScrollPane2, JSplitPane.TOP);
        jScrollPane2.getViewport().add(showThreadList, null);
        jSplitPane1.add(settingPanel, JSplitPane.RIGHT);
        settingPanel.add(jScrollPane3, new GridBagConstraints(0, 0, 10, 1, 1.0, 1.0
                , GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

        settingPanel.add(buttonPrevious,
                new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0
                        , GridBagConstraints.CENTER, GridBagConstraints.NONE,
                        new Insets(0, 5, 0, 0), 0, 0));
        settingPanel.add(buttonNext,
                new GridBagConstraints(5, 1, 1, 1, 0.0, 0.0
                        , GridBagConstraints.CENTER, GridBagConstraints.NONE,
                        new Insets(0, 0, 0, 0), 0, 0));


        settingPanel.add(threadDumpStatus, new GridBagConstraints(6, 1, 1, 1, 1.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 0), 0, 0));
        settingPanel.add(trashButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
                , GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        settingPanel.add(tableButton, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
                , GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
        settingPanel.add(fullButton, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
                , GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        settingPanel.add(sequenceButton, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0
                , GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 20), 0, 0));
        settingPanel.add(progressBar, new GridBagConstraints(7, 1, 1, 1, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        settingPanel.add(saveButton, new GridBagConstraints(8, 1, 1, 1, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        if (OSDetector.isMac() || OSDetector.isWindows()) {
            settingPanel.add(openButton, new GridBagConstraints(9, 1, 1, 1, 0.0, 0.0
                    , GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        }
        jScrollPane3.getViewport().add(threadDumpPanel, null);
        jSplitPane1.setDividerLocation(200);
        showThreadList.setFont(MainFrame.preservedFontToWorkaroundJPackageBug);

    }

    class RolloverBorder extends MouseAdapter {
        private final JButton button;

        RolloverBorder(JButton button) {
            this.button = button;
        }

        public void mouseEntered(MouseEvent event) {
            button.setBorderPainted(button.isEnabled());
        }

        public void mouseExited(MouseEvent event) {
            button.setBorderPainted(false);
        }

        public void mouseReleased(MouseEvent event) {
            button.setBorderPainted(button.isEnabled());
        }
    }

    public static File getTargetDirectory(File file) {
        String target = file.getAbsoluteFile().getParent();
        String fileName = file.getName();
        String directoryName;
        if (-1 == fileName.lastIndexOf(".")) {
            directoryName = fileName;
        } else {
            directoryName = fileName.substring(0, fileName.lastIndexOf("."));
        }
        target = target + File.separator + directoryName;
        File targetFile = new File(target);
        if (targetFile.exists()) {
            int count = 0;
            while (targetFile.exists()) {
                count++;
                targetFile = new File(target + "." + count);
            }
        }
        return targetFile;
    }

    File savedLocation = null;
    final JPanel THIS = this;

    final ActionListener buttonListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            Component source = (Component) e.getSource();
            if (source == saveButton) {
                if (saveButton.isEnabled()) {
                    synchronized (saveButton) {
                        if (saveButton.isEnabled()) {
                            saveButton.setEnabled(false);
                            progressBar.setString("");
                            progressBar.setVisible(true);
                            context.invokeLater(() -> {
                                VelocityHtmlRenderer renderer = new VelocityHtmlRenderer("one/cafebabe/samurai/web/outcss.vm", "../images/");
                                try {
                                    synchronized (statistic) {
                                        savedLocation = getTargetDirectory(currentFile);
                                        renderer.saveTo(statistic, savedLocation, (finished, all) -> SwingUtilities.invokeLater(new ProgressTask(finished, all + 9)));
                                        File imageDir = new File(savedLocation.getAbsolutePath() + "/images/");
                                        imageDir.mkdir();

                                        saveStreamAsFile(imageDir, "space.gif");
                                        SwingUtilities.invokeLater(new ProgressTask(progressBar.getMaximum() - 8, progressBar.getMaximum()));
                                        saveStreamAsFile(imageDir, "same-v.gif");
                                        SwingUtilities.invokeLater(new ProgressTask(progressBar.getMaximum() - 7, progressBar.getMaximum()));
                                        saveStreamAsFile(imageDir, "same-h.gif");
                                        SwingUtilities.invokeLater(new ProgressTask(progressBar.getMaximum() - 6, progressBar.getMaximum()));
                                        saveStreamAsFile(imageDir, "deadlocked.gif");
                                        SwingUtilities.invokeLater(new ProgressTask(progressBar.getMaximum() - 5, progressBar.getMaximum()));
                                        saveStreamAsFile(imageDir, "expandable_win.gif");
                                        SwingUtilities.invokeLater(new ProgressTask(progressBar.getMaximum() - 4, progressBar.getMaximum()));
                                        saveStreamAsFile(imageDir, "shrinkable_win.gif");
                                        SwingUtilities.invokeLater(new ProgressTask(progressBar.getMaximum() - 3, progressBar.getMaximum()));

                                        saveStreamAsFile(imageDir, "tableButton.gif");
                                        SwingUtilities.invokeLater(new ProgressTask(progressBar.getMaximum() - 2, progressBar.getMaximum()));
                                        saveStreamAsFile(imageDir, "fullButton.gif");
                                        SwingUtilities.invokeLater(new ProgressTask(progressBar.getMaximum() - 1, progressBar.getMaximum()));
                                        saveStreamAsFile(imageDir, "sequenceButton.gif");
                                        SwingUtilities.invokeLater(new ProgressTask(progressBar.getMaximum(), progressBar.getMaximum()));

                                    }
                                    context.setTemporaryStatus(resources.getMessage("ThreadDumpPanel.saved", savedLocation.getAbsolutePath()));
                                } catch (Exception ioe) {
                                    ioe.printStackTrace();
                                    SwingUtilities.invokeLater(new ProgressTask(progressBar.getMaximum(), progressBar.getMaximum()));
                                    context.setTemporaryStatus(ioe.getMessage());
                                } finally {
                                    context.invokeLater(() -> {
                                        progressBar.setVisible(false);
                                        progressBar.setValue(0);
                                        saveButton.setEnabled(true);
                                    }, 2);
                                }
                            });
                        }
                    }
                }
            } else if (source == trashButton) {
                if (JOptionPane.YES_OPTION ==
                        JOptionPane.showConfirmDialog(THIS, resources.getMessage("ThreadDumpPanel.confirmClear")
                                , resources.getMessage("ThreadDumpPanel.clear"), JOptionPane.YES_NO_OPTION)) {
                    synchronized (statistic) {
                        init();
                    }
                }
            } else if (source == openButton) {
                String[] command = null;
                if (OSDetector.isMac()) {
                    command = new String[]{"open", savedLocation.getAbsolutePath()};
                } else if (OSDetector.isWindows()) {
                    command = new String[]{"cmd.exe", "/C", "start", savedLocation.getAbsolutePath()};
                }
                try {
                    Runtime.getRuntime().exec(command);
                } catch (IOException ioe) {
                    context.setTemporaryStatus(ioe.getMessage());
                }
            } else {
                JToggleButton button = (JToggleButton) e.getSource();
                if (!button.isSelected()) {
                    button.setSelected(true);
                } else {
                    tableButton.setSelected((tableButton == button) == button.isSelected());
                    fullButton.setSelected((fullButton == button) == button.isSelected());
                    sequenceButton.setSelected((sequenceButton == button) == button.isSelected());
                    if (tableButton == button) {
                        filter.setMode(Constants.MODE_TABLE);
                    } else if (fullButton == button) {
                        filter.setMode(Constants.MODE_FULL);
                    } else if (sequenceButton == button) {
                        filter.setMode(Constants.MODE_SEQUENCE);
                    }
                    updateHtml();
                }
            }
        }

    };

    private void saveStreamAsFile(File parentDir, String fileName) throws IOException {
        try (InputStream is = ThreadDumpPanel.class.getResourceAsStream("/one/cafebabe/samurai/web/images/" + fileName);
             FileOutputStream fos = new FileOutputStream(parentDir.getAbsolutePath() + "/" + fileName)) {
            byte[] buf = new byte[256];
            int count;
            while (-1 != (count = is.read(buf))) {
                fos.write(buf, 0, count);
            }
        }
    }

    class ProgressTask implements Runnable {
        final int finished;
        final int all;

        ProgressTask(int finished, int all) {
            this.finished = finished;
            this.all = all;
        }

        public void run() {
            progressBar.setValue(finished);
            progressBar.setMaximum(all);
            if (!(finished == all)) {
                int progress = finished * 100 / all;
                progressBar.setString(progress + "%");
            } else {
                progressBar.setString("done.");
                openButton.setEnabled(true);
            }
        }
    }

    private String uri = "";

    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            uri = e.getDescription();
            if (uri.startsWith("#")) {
                threadDumpPanel.scrollToReference(e.getDescription().substring(1));
            } else {
                filter.setQuery(uri);
                updateHtml();
            }
        }
    }

    final GridBagLayout gridBagLayout1 = new GridBagLayout();
    final JButton buttonPrevious = new JButton();
    final JButton buttonNext = new JButton();

    private final ThreadFilter filter = new ThreadFilter();

    private ThreadDumpSequence[] threadList = null;

    public void changeBunttonFeel() {
        invokeLater(() -> {
            int selected = filter.getFullThreadIndex();
            if (0 == statistic.getFullThreadDumpCount()) {
                showThreadList.setEnabled(false);
                buttonPrevious.setEnabled(false);
                buttonNext.setEnabled(false);
            } else {
                showThreadList.setEnabled(true);
                if (filter.getMode().equals(Constants.MODE_FULL)) {
                    buttonPrevious.setEnabled(!(selected == 0));
                    buttonNext.setEnabled(!(statistic.getFullThreadDumpCount() - 1 == selected));
                } else {
                    buttonPrevious.setEnabled(Constants.MODE_SEQUENCE.equals(filter.getMode()));
                    buttonNext.setEnabled(Constants.MODE_SEQUENCE.equals(filter.getMode()));
                }
            }
            if (null != threadList && threadList.length != 0) {
                if (filter.getMode().equals(Constants.MODE_FULL)) {
                } else if (filter.getMode().equals(Constants.MODE_SEQUENCE)) {
                    for (int i = 0; i < threadList.length; i++) {
                        if (filter.getThreadId().equals(threadList[i].getId())) {
                            showThreadList.setSelectedIndex(i);
                            break;
                        }
                    }
                }
            }
            if (Constants.MODE_FULL.equals(filter.getMode())) {
                showThreadList.setSelectedIndices(new int[0]);
                threadDumpStatus.setText(filter.getFullThreadIndex() + 1 + "/" + statistic.getFullThreadDumpCount());
                tableButton.setSelected(false);
                fullButton.setSelected(true);
                sequenceButton.setSelected(false);
            } else if (Constants.MODE_TABLE.equals(filter.getMode())) {
                showThreadList.setSelectedIndices(new int[0]);
                threadDumpStatus.setText("");
                tableButton.setSelected(true);
                fullButton.setSelected(false);
                sequenceButton.setSelected(false);
            } else if (Constants.MODE_SEQUENCE.equals(filter.getMode())) {
                threadDumpStatus.setText(statistic.getStackTracesById(filter.getThreadId()).getName());
                tableButton.setSelected(false);
                fullButton.setSelected(false);
                sequenceButton.setSelected(true);
            }
            showThreadList.repaint();
        }
        );
    }

    private void updateHtml() {
        invokeLater(() -> {
            synchronized (statistic) {
                if (statistic.getFullThreadDumpCount() > 0) {
                    if ("".equals(filter.getThreadId())) {
                        filter.setThreadId(statistic.getStackTracesAsArray()[0].getId());
                    }
                    threadDumpPanel.setText(renderer.render(filter, statistic, velocityContext));
                    threadDumpPanel.select(0, 0);
                    if (uri.contains("#")) {
                        referer = uri.substring(uri.indexOf("#") + 1);
                    }
                    changeBunttonFeel();
                    for (int i = 0; i < threadList.length; i++) {
                        if ((threadList[i]).getName().equals(filter.getThreadId())) {
                            showThreadList.setSelectedIndex(i);
                        }
                    }

                }
            }
        });
    }

    final ThreadStatistic statistic = new ThreadStatistic() {
        private static final long serialVersionUID = 198789311977731508L;

        public synchronized void onFullThreadDump(FullThreadDump fullThreadDump) {
            super.onFullThreadDump(fullThreadDump);
            invokeLater(() -> {
                showMe(resources.getMessage("ThreadDumpPanel.threadDump"));
                threadList = statistic.getStackTracesAsArray();
                showThreadList.setListData(threadList);
                showThreadList.clearSelection();
            });
            updateHtml();
        }
    };
    private ThreadDumpExtractor analyzer = new ThreadDumpExtractor(statistic);
    final JSplitPane jSplitPane1 = new JSplitPane();
    final JScrollPane jScrollPane2 = new JScrollPane();
    final JScrollPane jScrollPane3 = new JScrollPane();
    final JList showThreadList = new JList();

    final JLabel threadDumpStatus = new JLabel();
    File currentFile;

    public void onLine(File file, String line, long filePointer) {
        super.onLine(file, line, filePointer);
        analyzer.analyzeLine(line);
    }

    public void logStarted(File file, long filePointer) {
        super.logStarted(file, filePointer);
        currentFile = file;
    }

    public void logEnded(File file, long filePointer) {
        super.logEnded(file, filePointer);
        analyzer.finish();
    }

    public synchronized void clearBuffer() {
        init();
        analyzer = new ThreadDumpExtractor(statistic);
        hideMe();
    }

    public synchronized void close() {
        super.close();
        clearBuffer();
    }

    private void init() {
        statistic.reset();
        filter.reset();
        threadDumpPanel.setText(resources.getMessage(
                "ThreadDumpPanel.threadDumpHere"));
        changeBunttonFeel();

//        showThreadList.setCellRenderer(new FontFixCellRenderer());
    }

    void showThreadList_actionPerformed(ActionEvent e) {
        if (showThreadList.getSelectedIndex() == 0) {
            filter.setMode(Constants.MODE_FULL);
        } else {
            filter.setMode(Constants.MODE_SEQUENCE);
            filter.setThreadId((String) showThreadList.getSelectedValue());
        }
        updateHtml();
    }

    public synchronized void onConfigurationChanged(Configuration config) {
        config.apply(renderer);
        velocityContext.setProperty("fontFamily", config_dumpFontFamily);
        velocityContext.setProperty("fontSize", config_dumpFontSize);
        updateHtml();
    }

    class ThreadCellRenderer extends JLabel implements ListCellRenderer {
        public ThreadCellRenderer() {
            setOpaque(true);
        }

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            ThreadDumpSequence threadDumps = ((ThreadDumpSequence) value);
            ThreadDump currentThreadDump = threadDumps.get(filter.getFullThreadIndex());

            Color fgColor = Color.BLACK;
            setFont(getFont().deriveFont(0));
            if (null != currentThreadDump) {
                if (Constants.MODE_FULL.equals(filter.getMode())) {
                    if (currentThreadDump.isIdle()) {
                        fgColor = Color.GRAY;
                    } else if (currentThreadDump.isBlocked()) {
                        fgColor = Color.RED;
                    }
                }
            } else {
                //no thread in this fullthreaddump
//        fgColor = Color.GRAY;
            }
            setText(value.toString());
            setBackground(isSelected ? fgColor : Color.white);
            setForeground(isSelected ? Color.white : fgColor);
            setFont(MainFrame.preservedFontToWorkaroundJPackageBug);
            return this;
        }
    }

    public void cut() {
    }

    public void copy() {
        this.threadDumpPanel.copy();
    }

    public void paste() {
    }
}
