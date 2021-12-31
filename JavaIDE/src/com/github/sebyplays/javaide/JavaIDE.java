package com.github.sebyplays.javaide;

import com.github.sebyplays.javaide.events.AddComponentTabEvent;
import com.github.sebyplays.javaide.utils.*;
import com.github.sebyplays.javaide.utils.Compiler;
import com.github.sebyplays.jevent.api.JEvent;
import com.github.sebyplays.jmodule.module.ModuleLoader;
import com.github.sebyplays.logmanager.api.LogType;
import com.google.googlejavaformat.java.Formatter;
import lombok.SneakyThrows;
import net.openhft.compiler.CompilerUtils;

import javax.rmi.CORBA.Util;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

public class JavaIDE extends JFrame {
    public JPanel contentPane, panel;
    public JTabbedPane codePane, outputPane;
    public JMenuBar jMenuBar;
    public JMenu mainMenu, fileMenu, settingsItem, buildItem;
    public JMenuItem openItem, saveItem, compileItem, newFileItem, exitItem, buildArtifactItem, runItem, clearCacheItem;
    public JScrollPane scrollPane;
    public JTree tree;
    public DefaultTreeModel defaultTreeModel;
    public DefaultMutableTreeNode root;

    public JTextArea outputArea, consoleLog;
    public JComponent currentlyEditing;
    public File projectDir, propertiesFile, selectedTreeFile;
    public Properties projectProperties;
    public JPopupMenu treeMenu;

    public Logger logger;

    public Highlighter highlighter;

    public Font font;

    public AutoComplete autoComplete = new AutoComplete();

    public int instanceId;

    public File sourceDir;

    public static ArrayList<JavaIDE> instances = new ArrayList<>();

    public boolean compiling = false;

    public static ModuleLoader moduleLoader;

    static {
        try {
            moduleLoader = new ModuleLoader("ideModules");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SneakyThrows
    public JavaIDE(File projectDir) {
        super("Java IDE");
        instances.add(this);
        instanceId = instances.size() - 1;
        setTitle("Java IDE {" + instanceId + "}" + " (" + projectDir.getAbsolutePath() + ")");
        addDefaultAutoCompletion();
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        logger = new Logger(this.getClass().getSimpleName(), true);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);

        highlighter = new Highlighter();
        addHighlights();

        panel = new JPanel();
        contentPane.add(panel, BorderLayout.CENTER);
        panel.setLayout(null);

        outputPane = new JTabbedPane(JTabbedPane.TOP);
        outputPane.setBounds(0, 314, 784, 115);
        panel.add(outputPane);
        JScrollPane outputScrollPane = new JScrollPane(new JScrollBar());
        JScrollPane consoleScrollPane = new JScrollPane(new JScrollBar());
        outputScrollPane.setViewportView(outputArea);
        consoleLog = new JTextArea();
        consoleLog.setEditable(false);
        consoleScrollPane.setViewportView(consoleLog);
        addComponentTab(outputPane, "Console", consoleScrollPane, "Software Log", null);
        insertLog(LogType.INFORMATION, "Logger components initialized..");
        insertLog(LogType.INFORMATION, "Initializing Java IDE..");
        insertLog(LogType.INFORMATION, "Loading project directory..");
        if(projectDir.isDirectory() && projectDir.listFiles().length > 0 && !directoryContains(projectDir, "project.properties")) {
            throw new IllegalArgumentException("Project directory already exists");
        }
        insertLog(LogType.INFORMATION, "Allocating project directory..");
        new File(projectDir, "src/").mkdirs();;
        new File(projectDir, "out/artifacts/").mkdirs();
        new File(projectDir, "libraries/").mkdirs();
        new File(projectDir, "out/production/log_dump/").mkdirs();
        insertLog(LogType.INFORMATION, "Project directory initialized!");
        this.projectProperties = new Properties();
        insertLog(LogType.INFORMATION, "Loading project properties..");
        if(!(propertiesFile = new File(projectDir, "project.properties")).exists()){
            insertLog(LogType.ERROR, "Project properties file not found!");
            insertLog(LogType.INFORMATION, "Creating project properties file..");
            propertiesFile.createNewFile();
            insertLog(LogType.INFORMATION, "Project properties file created!");
            this.projectProperties.load(new FileInputStream(new File(projectDir, "project.properties")));
            setProperties("name", projectDir.getName());
            setProperties("version", "1.0.0");
            setProperties("author", "Unknown");
            setProperties("description", "No description");
            setProperties("ide.font.size", "12");
            setProperties("ide.font.name", "Consolas");
            setProperties("ide.font.style", "0");
            setProperties("ide.font.color", "BLACK");
            setProperties("ide.font.background", "WHITE");
            setProperties("ide.setting.autosave", "false");
            setProperties("ide.setting.codeassist", "true");
            setProperties("ide.compile.production", projectDir.getAbsolutePath() + "/out/production/src/");
            setProperties("ide.compile.artifacts", projectDir.getAbsolutePath() + "/out/artifacts/");
            setProperties("ide.compile.dependencies", projectDir.getAbsolutePath() + "/libraries/");
            setProperties("ide.compile.logs", projectDir.getAbsolutePath() + "/out/production/log_dump/");
            setProperties("ide.compile.jre", "1.8");
            setProperties("ide.artifact.name", "Unknown");
            setProperties("ide.artifact.main", "Unknown");
            setProperties("ide.artifact.version", "Unknown");
            setProperties("ide.artifact.author", "Unknown");
            setProperties("ide.artifact.includeLibs", "false");
            setProperties("ide.run.programArguments", "");
            insertLog(LogType.INFORMATION, "Project properties initialized!");
        } else {
            insertLog(LogType.INFORMATION, "Project properties file found!");
            insertLog(LogType.INFORMATION, "Loading project properties..");
            this.projectProperties.load(new FileInputStream(new File(projectDir, "project.properties")));
            insertLog(LogType.INFORMATION, "Project properties loaded!");
        }
        this.font = new Font(getProperties("ide.font.name"), Integer.parseInt(getProperties("ide.font.style")), Integer.parseInt(getProperties("ide.font.size")));
        insertLog(LogType.INFORMATION, "Initializing IDE components..");


        this.projectDir = projectDir;
        this.sourceDir = new File(projectDir, "src/");
        setVisible(true);
        setResizable(false);
        setBounds(100, 100, 810, 500);

        jMenuBar = new JMenuBar();
        setJMenuBar(jMenuBar);
        mainMenu = new JMenu("Menu");
        fileMenu = new JMenu("File");
        JMenu aboutMenu = new JMenu("About");
        aboutMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("https://github.com/SebyPlays/JavaIDE"));
                } catch (Exception e2) {
                    insertLog(LogType.ERROR, "Failed to open link!" + e2.getMessage());
                }
            }
        });
        jMenuBar.add(mainMenu);
        jMenuBar.add(fileMenu);
        jMenuBar.add(aboutMenu);

        clearCacheItem = new JMenuItem("Clear Compiler Cache");
        clearCacheItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertToOutput("Clearing compiler cache..", true);
                insertLog(LogType.INFORMATION, "Clearing compiler cache..");
                CompilerUtils.CACHED_COMPILER.close();
                insertToOutput("Compiler cache cleared!", true);
                insertLog(LogType.INFORMATION, "Compiler cache cleared!");

            }
        });

        fileMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //text input dialog
                JTextField input = new JTextField();
                JOptionPane.showMessageDialog(null, input, "Input", JOptionPane.PLAIN_MESSAGE);
                String text = input.getText();
                insertLog(LogType.INFORMATION, "User input: " + text);
            }
        });

        newFileItem = new JMenuItem("New Project");
        newFileItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTextField input = new JTextField();
                input.setText(projectDir.getAbsolutePath());
                JOptionPane.showMessageDialog(null, input, "Enter a project directory path: ", JOptionPane.PLAIN_MESSAGE);
                new JavaIDE(new File(input.getText()));
            }
        });
        openItem = new JMenuItem("Open");
        openItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTextField input = new JTextField();
                input.setText(projectDir.getAbsolutePath());
                JOptionPane.showMessageDialog(null, input, "Enter a project directory path: ", JOptionPane.PLAIN_MESSAGE);
                new JavaIDE(new File(input.getText()));
            }
        });
        saveItem = new JMenuItem("Save");

        saveItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAll();
            }
        });


        settingsItem = new JMenu("Settings");

        JMenuItem compilerSettingsItem = new JMenuItem("Compiler Settings");
        compilerSettingsItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTextField artifactName = new JTextField();
                JTextField version = new JTextField();
                JTextField mainClass = new JTextField();
                JTextField author = new JTextField();
                JTextField jdkVersion = new JTextField();
                JTextField productionDir = new JTextField();
                JTextField artifactsDir = new JTextField();
                JTextField dependenciesDir = new JTextField();
                JTextField logsDir = new JTextField();
                JTextField programArgs = new JTextField();
                JCheckBox includeLibs = new JCheckBox("Include extracted libraries in final build?");

                artifactName.setText(getProperties("ide.artifact.name"));
                version.setText(getProperties("ide.artifact.version"));
                mainClass.setText(getProperties("ide.artifact.main"));
                author.setText(getProperties("ide.artifact.author"));
                jdkVersion.setText(getProperties("ide.compile.jre"));
                productionDir.setText(getProperties("ide.compile.production"));
                artifactsDir.setText(getProperties("ide.compile.artifacts"));
                dependenciesDir.setText(getProperties("ide.compile.dependencies"));
                logsDir.setText(getProperties("ide.compile.logs"));
                programArgs.setText(getProperties("ide.run.programArguments"));

                //joptionpane
                JOptionPane.showMessageDialog(null, new Object[]{
                        "Artifact Name", artifactName,
                        "Version", version,
                        "Main Class", mainClass,
                        "Author", author,
                        "JDK Version", jdkVersion,
                        "Production Directory", productionDir,
                        "Artifacts(Output) Directory", artifactsDir,
                        "Dependencies Directory", dependenciesDir,
                        "Logs Directory", logsDir,
                        "Program Arguments (When test-running the main class)", programArgs,
                        includeLibs},
                        "Compiler Settings", JOptionPane.PLAIN_MESSAGE);

                setProperties("ide.artifact.name", artifactName.getText());
                setProperties("ide.artifact.version", version.getText());
                setProperties("ide.artifact.main", mainClass.getText());
                setProperties("ide.artifact.author", author.getText());
                setProperties("ide.compile.jre", jdkVersion.getText());
                setProperties("ide.compile.production", productionDir.getText());
                setProperties("ide.compile.artifacts", artifactsDir.getText());
                setProperties("ide.compile.dependencies", dependenciesDir.getText());
                setProperties("ide.compile.logs", logsDir.getText());
                setProperties("ide.run.programArguments", programArgs.getText());
                setProperties("ide.artifact.includeLibs", includeLibs.isSelected() + "");

                if(artifactName.getText().equals("") || artifactName.getText().equals("Unknown") || artifactName.getText().equals(" ")){
                    actionPerformed(e);
                    return;
                }
                setProperties("ide.artifact.name", artifactName.getText());
                setProperties("ide.artifact.version", version.getText());
                setProperties("ide.artifact.main", mainClass.getText());
                setProperties("ide.artifact.author", author.getText());
            }
        });

        settingsItem.add(compilerSettingsItem);

        JMenuItem editorSettingsItem = new JMenuItem("Editor Settings");
        editorSettingsItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTextField fontSize = new JTextField();
                JTextField fontFamily = new JTextField();
                JTextField fontStyle = new JTextField();
                JTextField fontColor = new JTextField();
                JTextField backgroundColor = new JTextField();

                fontSize.setText(getProperties("ide.font.size"));
                fontFamily.setText(getProperties("ide.font.name"));
                fontStyle.setText(getProperties("ide.font.style"));
                fontColor.setText(getProperties("ide.font.color"));
                backgroundColor.setText(getProperties("ide.font.background"));

                //joptionpane
                JOptionPane.showMessageDialog(null, new Object[]{
                                "Font Size", fontSize,
                                "Font Family", fontFamily,
                                "Font Style", fontStyle,
                                "Font Color", fontColor,
                                "Background Color", backgroundColor},
                        "Editor Settings", JOptionPane.PLAIN_MESSAGE);

                setProperties("ide.font.size", fontSize.getText());
                setProperties("ide.font.name", fontFamily.getText());
                setProperties("ide.font.style", fontStyle.getText());
                setProperties("ide.font.color", fontColor.getText());
                setProperties("ide.font.background", backgroundColor.getText());
            }
        });

        settingsItem.add(editorSettingsItem);

        JMenuItem otherSettingsItem = new JMenuItem("Other Settings");
        otherSettingsItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JCheckBox codeassist = new JCheckBox();
                JCheckBox autosave = new JCheckBox();

                codeassist.setSelected(Boolean.parseBoolean(getProperties("ide.settings.codeassist")));
                autosave.setSelected(Boolean.parseBoolean(getProperties("ide.settings.autosave")));

                codeassist.setText("Code Assist(Tab-completion and auto-closing brackets)");
                autosave.setText("Autosave (Automatically saves a document, whenever the keyboard is used)");
                JOptionPane.showMessageDialog(null, new Object[]{ codeassist, autosave},
                        "Other Settings", JOptionPane.PLAIN_MESSAGE);

                setProperties("ide.settings.codeassist", codeassist.isSelected() + "");
                setProperties("ide.settings.autosave", autosave.isSelected() + "");
            }
        });

        settingsItem.add(otherSettingsItem);

        exitItem = new JMenu("Exit");
        exitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        fileMenu.add(exitItem);

        buildArtifactItem = new JMenuItem("Build Artifact");
        buildArtifactItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!compiling) {
                    preCompileDialog();
                    String artifact = getProperties("ide.artifact.name");
                    String ver = getProperties("ide.artifact.version");
                    String main = getProperties("ide.artifact.main");
                    String auth = getProperties("ide.artifact.author");
                    insertLog(LogType.INFORMATION, "Compiling project with settings: " + artifact + " " + ver + " " + main);

                    clearOutput();
                    insertLog(LogType.INFORMATION, "Compiling project..");
                    insertToOutput("Compiling project..", true);

                    Compiler compiler = new Compiler(instances.get(instanceId), sourceDir, new File(getProperties("ide.compile.production")),
                            new File(getProperties("ide.compile.artifacts")),
                            new File(getProperties("ide.compile.logs")),
                            new File(getProperties("ide.compile.dependencies")), getProperties("ide.compile.jre"));

                    compiler.setArtifactName(artifact).setArtifactVersion(ver).setArtifactMainClass(main).setArtifactAuthor(auth);
                    compiler.compileProject(true, getProperties("ide.artifact.includeLibs").equals("true"));
                } else {
                    insertLog(LogType.INFORMATION, "Compiler is already active!");
                    insertToOutput("Compiler is already active!", true);
                }
            }
        });

        mainMenu.add(clearCacheItem);

        compileItem = new JMenuItem("Compile");
        compileItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                preCompileDialog();
                String artifact = getProperties("ide.artifact.name");
                String ver = getProperties("ide.artifact.version");
                String main = getProperties("ide.artifact.main");
                String auth = getProperties("ide.artifact.author");
                insertLog(LogType.INFORMATION, "Compiling project with settings: " + artifact + " " + ver + " " + main);

                clearOutput();
                insertLog(LogType.INFORMATION, "Compiling project..");
                insertToOutput("Compiling project..", true);

                Compiler compiler = new Compiler(instances.get(instanceId), sourceDir, new File(getProperties("ide.compile.production")),
                        new File(getProperties("ide.compile.artifacts")),
                        new File(getProperties("ide.compile.logs")),
                        new File(getProperties("ide.compile.dependencies")), getProperties("ide.compile.jre"));

                compiler.setArtifactName(artifact).setArtifactVersion(ver).setArtifactMainClass(main).setArtifactAuthor(auth);
                compiler.compileProject(false, false);
            }
        });

        runItem = new JMenuItem("Run");
        runItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(Utils.equalsIgnoreMultiple(getProperties("ide.artifact.main"), "", "null", "undefined", "unknown", " ")){
                    insertLog(LogType.INFORMATION, "No main class defined!");
                    insertToOutput("No main class defined!", true);
                    JTextField mainClass = new JTextField();

                    JOptionPane.showMessageDialog(null, mainClass, "Enter main class", JOptionPane.QUESTION_MESSAGE);
                    if(!Utils.equalsIgnoreMultiple(mainClass.getText(), "", "null", "undefined", "unknown", " ")){
                        setProperties("ide.artifact.main", mainClass.getText());
                    }
                }
                Utils.invokeMain(new File(getProperties("ide.compile.production")), getProperties("ide.artifact.main"),  getProperties("ide.run.programArguments").split(" "));
            }
        });
        buildItem = new JMenu("Build");
        buildItem.add(runItem);
        buildItem.add(buildArtifactItem);
        buildItem.add(compileItem);


        fileMenu.add(newFileItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);

        mainMenu.add(settingsItem);
        mainMenu.add(buildItem);

        codePane = new JTabbedPane(JTabbedPane.TOP);
        codePane.setBounds(181, 0, 603, 308);

        panel.add(codePane);
        addClassTab("Welcome.class", "public class Welcome {\n\n    public Welcome(){\n        System.out.println(\"Hello World\");\n    }\n\n}");

        scrollPane = new JScrollPane();
        scrollPane.setBounds(10, 0, 161, 308);
        panel.add(scrollPane);

        tree = new JTree();
        tree.setBounds(10, 0, 161, 308);
        panel.add(tree);

        root = new DefaultMutableTreeNode(projectDir.getName() + " (" + projectDir.getAbsolutePath() + ")");
        defaultTreeModel = new DefaultTreeModel(root);
        tree.setModel(defaultTreeModel);

        tree.addMouseListener(new java.awt.event.MouseAdapter() {
            @SneakyThrows
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                String path = projectDir.getAbsolutePath() + "\\";
                for (Object o : ((DefaultMutableTreeNode)tree.getLastSelectedPathComponent()).getPath()) {
                    if(!o.equals(((DefaultMutableTreeNode) tree.getLastSelectedPathComponent()).getPath()[0]))
                        path += o.toString() + File.separator;
                };
                System.out.println(path);
                selectedTreeFile = new File(path);
                insertLog(LogType.INFORMATION, "File selected: " + selectedTreeFile.getName());
                if(selectedTreeFile != null) {
                    if(selectedTreeFile.isFile()){
                        for(int i = 0; i < codePane.getTabCount(); i++) {
                            if(codePane.getTitleAt(i).equals(selectedTreeFile.getName())) {
                                codePane.setSelectedIndex(i);
                                return;
                            }
                        }

                        if(selectedTreeFile.getName().endsWith(".java")) {
                            addClassTab(selectedTreeFile.getName(), new String(Files.readAllBytes(selectedTreeFile.toPath())));
                            insertLog(LogType.INFORMATION, "File added to code pane: " + selectedTreeFile.getName());
                            return;
                        }

                        //if file is image, show image
                        if(selectedTreeFile.getName().endsWith(".png") || selectedTreeFile.getName().endsWith(".jpg") || selectedTreeFile.getName().endsWith(".jpeg")) {
                            JLabel label = new JLabel();
                            label.setIcon(new ImageIcon(selectedTreeFile.getAbsolutePath()));
                            addComponentTab(codePane, selectedTreeFile.getName(), label, "Image", null);
                            return;
                        }

                        if(selectedTreeFile.getName().endsWith(".class")) {
                            JEditorPane editor = new JEditorPane();
                            editor.setEditable(false);
                            editor.setContentType("text/java");
                            addClassTab(selectedTreeFile.getName(), Utils.decompile(editor, selectedTreeFile), editor);
                        }
                        insertLog(LogType.WARNING, "File not supported!");
                    }
                }
            }
        });


        treeMenu = new JPopupMenu();
        JMenu treeMenuNewItem = new JMenu("New");
        JMenuItem treeNewPackageItem = new JMenuItem("Package");
        treeNewPackageItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String name = JOptionPane.showInputDialog(null, "Enter package name");
                if(name != null && selectedTreeFile.isDirectory()) {
                    File newPackage = new File(selectedTreeFile.getAbsolutePath() + "\\" + name);
                    if(!newPackage.exists()) {
                        newPackage.mkdirs();
                        insertLog(LogType.INFORMATION, "Package created: " + newPackage.getAbsolutePath());
                        ((DefaultMutableTreeNode) tree.getLastSelectedPathComponent()).add(new DefaultMutableTreeNode(name));
                        defaultTreeModel.reload();
                        return;
                    }
                    insertLog(LogType.WARNING, "Package already exists: " + newPackage.getAbsolutePath());
                    JOptionPane.showMessageDialog(null, "Package already exists");
                }
            }
        });

        JMenuItem treeNewClassItem = new JMenuItem("Class");
        treeNewClassItem.addActionListener(new ActionListener() {
            @SneakyThrows
            @Override
            public void actionPerformed(ActionEvent e) {
                String name = JOptionPane.showInputDialog(null, "Enter class name");
                if(name != null && selectedTreeFile.isDirectory()) {
                    File newClass = new File(selectedTreeFile.getAbsolutePath() + "\\" + name + ".java");
                    if(!newClass.exists()) {
                        newClass.createNewFile();
                        ((DefaultMutableTreeNode) tree.getLastSelectedPathComponent()).add(new DefaultMutableTreeNode(name + ".java"));
                        FileWriter fw = new FileWriter(newClass);
                        String pkg = null;
                        try {
                            pkg = selectedTreeFile.getAbsolutePath().substring(selectedTreeFile.getAbsolutePath().indexOf("src") +4);
                            pkg = pkg.replace(File.separator, ".");
                        } catch (Exception e1) {
                            insertLog(LogType.ERROR, "Error while creating class: " + e1.getMessage());
                        }
                        if(pkg != null && !pkg.equals(""))
                            fw.write("package " + pkg + ";\n\n");
                        fw.write("public class " + name + " {\n\n}");
                        fw.close();
                        insertLog(LogType.INFORMATION, "Class created: " + newClass.getAbsolutePath());
                        addClassTab(name + ".java", new String(Files.readAllBytes(newClass.toPath())));
                        codePane.setSelectedIndex(codePane.getTabCount() - 1);
                        return;
                    }
                    insertLog(LogType.WARNING, "Class already exists: " + newClass.getAbsolutePath());
                    JOptionPane.showMessageDialog(null, "Class already exists");
                }
            }
        });

        JMenuItem treeNewFileItem = new JMenuItem("File");
        treeMenuNewItem.add(treeNewPackageItem);
        treeMenuNewItem.add(treeNewClassItem);
        treeMenuNewItem.add(treeNewFileItem);

        treeMenu.add(treeMenuNewItem);



        JMenuItem treeMenuOpenItem = new JMenuItem("Open In Editor");
        treeMenuOpenItem.addActionListener(new ActionListener() {
            @SneakyThrows
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedTreeFile.isFile()){
                    if(selectedTreeFile.getName().endsWith(".class")) {
                        JEditorPane editor = new JEditorPane();
                        editor.setEditable(false);
                        editor.setContentType("text/java");
                        addClassTab(selectedTreeFile.getName(), Utils.decompile(editor, selectedTreeFile), editor);
                    }  else {
                        addClassTab(selectedTreeFile.getName(), new String(Files.readAllBytes(selectedTreeFile.toPath())));
                    }
                }
            }
        });
        JMenuItem treeMenuOpenInExplorerItem = new JMenuItem("Open in Explorer");
        treeMenuOpenInExplorerItem.addActionListener(new ActionListener() {
            @SneakyThrows
            @Override
            public void actionPerformed(ActionEvent e) {
                insertLog(LogType.INFORMATION, "Opening file in explorer: " + selectedTreeFile.getAbsolutePath());
                Desktop.getDesktop().open(selectedTreeFile);
            }
        });
        JMenuItem treeMenuDeleteItem = new JMenuItem("Delete");
        treeMenuDeleteItem.addActionListener(new ActionListener() {
            @SneakyThrows
            @Override
            public void actionPerformed(ActionEvent e) {
                insertLog(LogType.INFORMATION, "Deleting file: " + selectedTreeFile.getAbsolutePath());
                if(selectedTreeFile.isDirectory()) {
                    for(File file : getFiles(selectedTreeFile)) {
                        file.delete();
                        insertLog(LogType.INFORMATION, "Deleted file: " + file.getAbsolutePath());
                    }
                    for(File file : getFiles(selectedTreeFile)) {
                        file.delete();
                        insertLog(LogType.INFORMATION, "Deleted file: " + file.getAbsolutePath());
                    }
                }
                selectedTreeFile.delete();
                ((DefaultMutableTreeNode)tree.getLastSelectedPathComponent()).removeAllChildren();;
                ((DefaultMutableTreeNode)tree.getLastSelectedPathComponent()).removeFromParent();

            }
        });

        JMenuItem treeMenuReloadItem = new JMenuItem("Reload");
        treeMenuReloadItem.addActionListener(new ActionListener() {
            @SneakyThrows
            @Override
            public void actionPerformed(ActionEvent e) {
                insertLog(LogType.INFORMATION, "Reloading Tree!");
                reloadTree();
            }
        });
        JMenuItem treeMenuRenameItem = new JMenuItem("Rename");
        treeMenuRenameItem.addActionListener(new ActionListener() {
            @SneakyThrows
            @Override
            public void actionPerformed(ActionEvent e) {
                String newName = JOptionPane.showInputDialog("Enter new name");
                if(newName != null) {
                    insertLog(LogType.INFORMATION, "Renaming file: " + selectedTreeFile.getAbsolutePath() + " to " + newName);
                    selectedTreeFile.renameTo(new File(selectedTreeFile.getParent() + "\\" + newName));
                    ((DefaultMutableTreeNode) tree.getLastSelectedPathComponent()).setUserObject(newName);
                }
            }
        });

        treeMenu.add(treeMenuOpenItem);
        treeMenu.add(treeMenuOpenInExplorerItem);
        treeMenu.add(treeMenuDeleteItem);
        treeMenu.add(treeMenuRenameItem);
        treeMenu.add(treeMenuReloadItem);

        tree.setComponentPopupMenu(treeMenu);

        loadNodes(projectDir, root);

        codePane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if(codePane.getSelectedIndex() == -1)
                    return;
                currentlyEditing = (JComponent) (((JScrollPane)codePane.getSelectedComponent()).getViewport().getView());
                if(currentlyEditing instanceof JEditorPane) {
                    System.out.println(((JEditorPane)currentlyEditing).getText());
                }
            }
        });

        scrollPane.setViewportView(tree);
        scrollPane.setVerticalScrollBar(new JScrollBar());


        outputArea = new JTextArea();
        outputArea.setEditable(false);

        outputScrollPane = new JScrollPane();
        outputScrollPane.setViewportView(outputArea);

        addComponentTab(outputPane, "Output", outputScrollPane, "This is, where all the magic will happen.", null);
        moduleLoader.loadModules();
    }

    public void addComponentTab(JTabbedPane jTabbedPane, String name, JComponent component, String tooltip, Icon icon) {
        insertLog(LogType.INFORMATION, "Adding tab: " + name);
        if(new JEvent(new AddComponentTabEvent(this, jTabbedPane, name, component, tooltip, icon)).callEvent().getEvent().isCancelled())
            return;
        JScrollPane scrollPanel = new JScrollPane(new JScrollBar());;
        scrollPanel.setViewportView(component);
        //add popupmenu to tab
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem closeItem = new JMenuItem("Close");

        closeItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertLog(LogType.INFORMATION, "Closing tab: " + name);
                codePane.remove(codePane.getSelectedIndex());
            }
        });
        popupMenu.add(closeItem);
        if(component.getComponentPopupMenu() != null){
            component.getComponentPopupMenu().add(closeItem);
        } else
            component.setComponentPopupMenu(popupMenu);
        jTabbedPane.addTab(name, icon, scrollPanel, tooltip);
    }

    public void addClassTab(String name, String content){
        JEditorPane editorPane = new JEditorPane();
        addClassTab(name, content, editorPane);
    }

    public void addClassTab(String name, String content, JEditorPane editorPane) {
        JScrollPane scrollPanel = new JScrollPane(new JScrollBar());
        //set font of editor pane
        editorPane.setFont(font);
        editorPane.setForeground(Color.getColor(getProperties("ide.font.color")));
        editorPane.setBackground(Color.getColor(getProperties("ide.font.background")));
        highlighter.applyHighlighter(editorPane);
        editorPane.setDocument(new DefaultStyledDocument());
        editorPane.setText(content);
        scrollPanel.setViewportView(editorPane);
        //add popupmenu to tab
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem copyItem = new JMenuItem("Copy");
        JMenuItem pasteItem = new JMenuItem("Paste");
        JMenuItem saveItem = new JMenuItem("Save");
        JMenuItem placeholder = new JMenuItem("");
        JMenuItem formatItem = new JMenuItem("Format Code");
        JMenuItem cacheCompile = new JMenuItem("Cache Compile");
        JMenuItem cacheCompileNRun = new JMenuItem("Cache Compile and Run");

        cacheCompileNRun.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Class c = CompilerUtils.CACHED_COMPILER.loadFromJava(Compiler.getPackage(editorPane.getText()) + "." + Compiler.getClassName(editorPane.getText()).replaceAll(" ", ""), editorPane.getText());
                    c.newInstance();
                } catch (Exception e1) {
                    insertLog(LogType.ERROR, "Failed to cache compile: " + e1.getMessage());
                    insertToOutput(e1.getMessage(), true);
                }
            }
        });
        cacheCompile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    CompilerUtils.CACHED_COMPILER.loadFromJava(Compiler.getPackage(editorPane.getText()) + "." + Compiler.getClassName(editorPane.getText()), editorPane.getText());
                } catch (Exception e1) {
                    insertLog(LogType.ERROR, "Failed to cache compile: " + e1.getMessage());
                    insertToOutput(e1.getMessage(), true);
                }
            }
        });

        formatItem.addActionListener(new ActionListener() {
            @SneakyThrows
            @Override
            public void actionPerformed(ActionEvent e) {
                insertLog(LogType.INFORMATION, "Formatting code");
                editorPane.setText(new Formatter().formatSource(editorPane.getText()));
            }
        });

        saveItem.addActionListener(new ActionListener() {
            @SneakyThrows
            @Override
            public void actionPerformed(ActionEvent e) {
                insertLog(LogType.INFORMATION, "Saving file: " + name);
                Files.write(selectedTreeFile.toPath(), editorPane.getText().getBytes());
            }
        });

        copyItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                StringSelection selection = new StringSelection(editorPane.getSelectedText());
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(selection, selection);
                insertLog(LogType.INFORMATION, "Copied: " + editorPane.getSelectedText());
            }
        });
        pasteItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                Transferable contents = clipboard.getContents(clipboard);
                try {
                    if (contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        String text = (String) contents.getTransferData(DataFlavor.stringFlavor);
                        editorPane.replaceSelection(text);
                        insertLog(LogType.INFORMATION, "Pasted: " + text);
                    }
                } catch (UnsupportedFlavorException | IOException ex) {
                    insertLog(LogType.ERROR, "Error pasting: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });
        popupMenu.add(copyItem);
        popupMenu.add(pasteItem);
        popupMenu.add(saveItem);
        popupMenu.add(placeholder);
        popupMenu.add(formatItem);
        popupMenu.add(cacheCompile);
        popupMenu.add(cacheCompileNRun);

        editorPane.setComponentPopupMenu(popupMenu);
        editorPane.addKeyListener(new KeyAdapter() {
            char previousCharacter;
              @Override
              public void keyPressed(KeyEvent e) {
                  if(e.getKeyCode() == KeyEvent.VK_S && e.isControlDown())
                      saveItem.doClick();
                  if(e.getKeyCode() == KeyEvent.VK_C && e.isControlDown())
                      copyItem.doClick();
                  if(e.getKeyCode() == KeyEvent.VK_V && e.isControlDown())
                      pasteItem.doClick();

                  if(e.getKeyCode() == KeyEvent.VK_F && e.isControlDown())
                      formatItem.doClick();

                  if(e.getKeyCode() == KeyEvent.VK_UP && e.isControlDown() && e.isShiftDown()){
                      int start = editorPane.getSelectionStart();
                      int end = editorPane.getSelectionEnd();
                      String text = editorPane.getText();
                      String[] lines = text.split("\n");
                      StringBuilder sb = new StringBuilder();
                      for(int i = 0; i < lines.length; i++){
                          if(i == 0){
                              sb.append(lines[i]);
                          }
                          if(i == lines.length - 1){
                              sb.append(lines[i]);

                          }
                          if(i > 0 && i < lines.length - 1){
                              sb.append("\n");
                              sb.append(lines[i]);
                          }

                      }
                      editorPane.setText(sb.toString());
                      editorPane.setSelectionStart(start);
                      editorPane.setSelectionEnd(end);

                  }

                  if(e.getKeyCode() == KeyEvent.VK_DOWN && e.isControlDown() && e.isShiftDown()){

                  }

                  if(e.getKeyCode() == KeyEvent.VK_TAB)
                      e.consume();

                  if(e.getKeyCode() == KeyEvent.VK_T && e.isControlDown() && e.isShiftDown()){
                      e.consume();
                      editorPane.replaceSelection(autoComplete.processRequest(editorPane.getSelectedText().toLowerCase()));
                  }

              }

            @Override
            public void keyReleased(KeyEvent e) {

                if(e.getKeyCode() == KeyEvent.VK_TAB){
                    String resultOfProcess = autoComplete.processRequest(editorPane.getSelectedText());
                    if(resultOfProcess == null || resultOfProcess.equals(""))
                        editorPane.replaceSelection("    ");
                    else
                        editorPane.replaceSelection(resultOfProcess);

                }
                boolean isOneOf = (e.getKeyChar() == '\'' || e.getKeyChar() == '"' || e.getKeyChar() == '(' || e.getKeyChar() == '{' || e.getKeyChar() == '[');
                if(Boolean.parseBoolean(getProperties("ide.setting.codeassist"))){
                    if(e.getKeyChar() == '{')
                        editorPane.replaceSelection("}");
                    if(e.getKeyChar() == '[')
                        editorPane.replaceSelection("]");
                    if(e.getKeyChar() == '(')
                        editorPane.replaceSelection(")");
                    if(e.getKeyChar() == '"')
                        editorPane.replaceSelection("\"");
                    if(e.getKeyChar() == '\'')
                        editorPane.replaceSelection("\'");
                    if(isOneOf)
                        editorPane.setCaretPosition(editorPane.getCaretPosition() - 1);

                }
                previousCharacter = e.getKeyChar();

            }

            @Override
            public void keyTyped(KeyEvent e) {
                if(Boolean.parseBoolean(getProperties("ide.setting.autosave")))
                    saveItem.doClick();
            }
        });
        addComponentTab(codePane, name, editorPane, "", null);
    }


    public void loadNodes(File rootDir, DefaultMutableTreeNode rootNode) {
        insertLog(LogType.INFORMATION, "Loading nodes from: " + rootDir.getAbsolutePath());
        File[] files = rootDir.listFiles();
        if (files == null) return;
        for (File file : files) {
            insertLog(LogType.INFORMATION, "Loading file: " + file.getName());
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(file.getName());
            rootNode.add(childNode);
            if (file.isDirectory()) {
                loadNodes(file, childNode);
            }
        }
    }

    public boolean directoryContains(File rootDir, String name) {
        for(File file : rootDir.listFiles())
            if(file.getName().equalsIgnoreCase(name))
                return true;
        return false;
    }

    public File[] getFiles(File rootDir) {
        ArrayList<File> files = new ArrayList<>();
        files.add(rootDir);
        for(File file : rootDir.listFiles()){
            if(file.isDirectory())
                files.addAll(Arrays.asList(getFiles(file)));
            else
                files.add(file);
        }
        return files.toArray(new File[files.size()]);
    }

    @SneakyThrows
    public void setProperties(String key, String value) {
        insertLog(LogType.INFORMATION, "Setting property: " + key + " to " + value);
        projectProperties.setProperty(key, value);
        projectProperties.store(new FileOutputStream(propertiesFile), null);
    }

    public String getProperties(String key) {
        return projectProperties.getProperty(key);
    }

    @SneakyThrows
    public static void main(String[] args) {
        JTextField textField = new JTextField();
        textField.setText(System.getProperty("user.dir"));
        JOptionPane.showMessageDialog(null, textField, "Enter a project directory", JOptionPane.QUESTION_MESSAGE);
        new JavaIDE(new File(textField.getText()));
    }


    @SneakyThrows
    public void insertLog(LogType type, String message) {
        if(type == LogType.ERROR) {
            consoleLog.setForeground(Color.RED);
            logger.error(message);
        } else if(type == LogType.WARNING){
            consoleLog.setForeground(Color.ORANGE);
            logger.warning(message);
        } else if(type == LogType.INFORMATION){
            consoleLog.setForeground(Color.BLACK);
            logger.info(message);
        } else if(type == LogType.NORMAL){
            consoleLog.setForeground(Color.BLACK);
            logger.normal(message);
        }
        consoleLog.append(message + "\n");
    }


    public void reloadTree(){
        insertLog(LogType.INFORMATION, "Reloading tree");
        insertLog(LogType.INFORMATION, "Clearing tree");
        tree.removeAll();
        root = new DefaultMutableTreeNode(projectDir.getName() + " (" + projectDir.getAbsolutePath() + ")");
        defaultTreeModel = new DefaultTreeModel(root);
        tree.setModel(defaultTreeModel);
        loadNodes(projectDir, root);
    }

    //<editor-fold desc="Getters and Setters">
    public void addHighlights(){
        highlighter.add("public", new Color(157, 107, 49));
        highlighter.add("void", new Color(157, 107, 49));
        highlighter.add("\\n", new Color(157, 107, 49));
        highlighter.add(",", new Color(157, 107, 49));
        highlighter.add(";", new Color(157, 107, 49));
        highlighter.add("new", new Color(157, 107, 49));
        highlighter.add("extends", new Color(157, 107, 49));
        highlighter.add("implements", new Color(157, 107, 49));
        highlighter.add("\"", new Color(76, 121, 86));
        highlighter.add("\'", new Color(76, 121, 86));
        highlighter.add("//", new Color(126, 111, 83));
    }

    public void saveAll(){
        for(int i = 0; i < codePane.getTabCount(); i++){
            JScrollPane scrollPane = (JScrollPane) codePane.getComponentAt(i);
            JEditorPane editorPane = (JEditorPane) scrollPane.getViewport().getView();
            JMenuItem save = (JMenuItem) editorPane.getComponentPopupMenu().getComponent(2);
            save.doClick();
        }
    }

    public void addDefaultAutoCompletion(){
        autoComplete.add("main", "public static void main(String[] args){\n}");
        autoComplete.add("sysout", "System.out.println(\"\");");
        autoComplete.add("syserr", "System.err.println(\"\");");
        autoComplete.add("sysin", "System.in.read();");
        autoComplete.add("if", "if(true){\n}");
        autoComplete.add("pu", "public");
        autoComplete.add("pr", "private");
        autoComplete.add("vo", "void");
        autoComplete.add("str", "String");
        autoComplete.add("int", "Integer");
        autoComplete.add("bool", "boolean");
        autoComplete.add("for", "for(int i = 0; i < 10; i++){\n}");
        autoComplete.add("while", "while(true){\n}");
        autoComplete.add("do", "do{\n}while(true);");
        autoComplete.add("try", "try{\n}catch(Exception e){\n}");
        autoComplete.add("catch", "catch(Exception e){\t\n}");
        autoComplete.add("th", "throws");
        autoComplete.add("new", "new ");
        autoComplete.add("ext", "extends");
        autoComplete.add("imp", "implements");
        autoComplete.add("this", "this.");
        autoComplete.add("swi", "switch(){\n\tcase:\n}");
    }

    public void insertToOutput(String text, boolean newLine){
        if(newLine){
            outputArea.append(text + "\n");
            return;
        }
        outputArea.append(text);
    }

    public void clearOutput(){
        outputArea.setText("");
    }

    public void preCompileDialog(){
        boolean includeLibraries = getProperties("ide.artifact.includeLibs").equals("true");
        if(getProperties("ide.artifact.name").equals("Unknown") ||
                getProperties("ide.artifact.version").equals("Unknown") ||
                getProperties("ide.artifact.main").equals("Unknown")){

            JTextField artifactName = new JTextField();
            JTextField version = new JTextField();
            JTextField mainClass = new JTextField();
            JTextField author = new JTextField();
            JCheckBox includeLibrariescb = new JCheckBox();

            includeLibrariescb.setSelected(includeLibraries);
            includeLibrariescb.setText("Include extracted libraries in build");
            JOptionPane.showMessageDialog(null,
                    new Object[]{"Leave Version and Author empty if no Main is specified!",
                            "Artifact name:", artifactName,
                            "Main Class:", mainClass,
                            "Version:", version,
                            "Author:", author, includeLibrariescb}, "Compile settings", JOptionPane.PLAIN_MESSAGE);


            includeLibraries = includeLibrariescb.isSelected();
            setProperties("ide.artifact.name", artifactName.getText());
            setProperties("ide.artifact.version", version.getText());
            setProperties("ide.artifact.main", mainClass.getText());
            setProperties("ide.artifact.author", author.getText());
            setProperties("ide.artifact.includeLibs", includeLibrariescb.isSelected() + "");
        }
    }


}