package com.github.sebyplays.javaide.utils;

import com.github.sebyplays.javaide.JavaIDE;
import com.github.sebyplays.logmanager.api.LogType;
import com.strobel.decompiler.languages.java.JavaOutputVisitor;
import lombok.Getter;
import lombok.SneakyThrows;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.CompletableFuture;
import java.util.jar.*;
import java.util.regex.Matcher;

public class Compiler {

    // compile input class file

    @Getter private File inputDir;
    @Getter private File outputDir;
    @Getter private File logsDir;
    @Getter private File depsDir;
    @Getter private File productionDir;
    @Getter private String jreVersion;
    @Getter private String artifactName = null;
    @Getter private String artifactMainClass = null;
    @Getter private String artifactVersion = null;
    @Getter private String artifactAuthor = null;
    @Getter private JavaIDE javaIDE;
    @Getter private File lastError;
    @Getter private boolean overrideDuplicatesWhenCompiling = false;


    public Compiler(JavaIDE ide ,File inputDirectory, File productionDir, File outputDirectory, File logsDir, File dependenciesDirectory, String jreVersion) {
        this.inputDir = inputDirectory;
        this.outputDir = outputDirectory;
        this.logsDir = logsDir;
        this.depsDir = dependenciesDirectory;
        this.productionDir = productionDir;
        this.jreVersion = jreVersion;
        this.javaIDE = ide;
        if(!productionDir.exists())
            productionDir.mkdirs();
        if(!outputDirectory.exists())
            outputDirectory.mkdirs();
        if(!logsDir.exists())
            logsDir.mkdirs();
    }


    public void compileProject(boolean putToJar, boolean includeLibs) {
        compileProject(inputDir, putToJar, includeLibs);
    }

    public Compiler setArtifactName(String artifactName) {
        this.artifactName = artifactName;
        return this;
    }

    public Compiler setArtifactMainClass(String artifactMainClass) {
        if(!artifactMainClass.equals("") && !artifactMainClass.equals("Unknown"))
            this.artifactMainClass = artifactMainClass;
        return this;
    }

    public Compiler setArtifactVersion(String artifactVersion) {
        if(!artifactVersion.equals("") && !artifactVersion.equals("Unknown"))
            this.artifactVersion = artifactVersion;
        return this;
    }

    public Compiler setArtifactAuthor(String artifactAuthor) {
        if(!artifactAuthor.equals("") && !artifactAuthor.equals("Unknown"))
            this.artifactAuthor = artifactAuthor;
        return this;
    }

    @SneakyThrows
    private void compileProject(File start, boolean putToJar, boolean includeLibs) {
        javaIDE.compiling = true;
        for (File file : javaIDE.getFiles(start)) {
            if(file.getName().endsWith(".java")) {
                int ec = compile(file);
                if(ec != 0) {
                    javaIDE.insertToOutput("Compilation failed for file: " + file.getName(), true);
                    javaIDE.insertLog(LogType.ERROR ,"Compilation failed for file: " + file.getName());
                    javaIDE.insertToOutput(new String(Files.readAllBytes(lastError.toPath())), true);
                    javaIDE.insertLog(LogType.ERROR ,new String(Files.readAllBytes(lastError.toPath())));
                    javaIDE.compiling = false;
                    return;
                }
            }
        }
        if(artifactName == null)
            throw new RuntimeException("Artifact name not set");

        if(putToJar){
            File jarFile = new File(outputDir.getAbsolutePath() + Matcher.quoteReplacement(File.separator) + artifactName + ".jar");
            JarOutputStream jarOutputStream = repackDirectoryToJarFile(productionDir, artifactName);
            if(includeLibs)
                repackDependenciesToJarFile(jarOutputStream);
            jarOutputStream.close();
            Utils.openDirectory(jarFile.getParentFile().getAbsolutePath());
        }
        javaIDE.compiling = false;

    }

    @SneakyThrows
    public boolean repackDependenciesToJarFile(JarOutputStream jarOutputStream) {

        for (File file : javaIDE.getFiles(depsDir)) {
            if (file.getName().endsWith(".jar")) {
                JarFile jarFile = new JarFile(file);
                try {
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        InputStream is = jarFile.getInputStream(entry);
                        try {
                            jarOutputStream.putNextEntry(new JarEntry(entry.getName()));
                        } catch (IOException e) {
                            if(!overrideDuplicatesWhenCompiling)
                                continue;
                        }
                        byte[] buffer = new byte[4096];
                        int bytesRead = 0;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            jarOutputStream.write(buffer, 0, bytesRead);
                        }
                        is.close();
                    }
                } catch (Exception e) {
                    javaIDE.insertToOutput("Error while repacking dependencies to jar file: " + e.getMessage(), true);
                    javaIDE.insertLog(LogType.ERROR ,"Error while repacking dependencies to jar file: " + e.getMessage());
                    return false;
                }
            }

        }
        return true;
    }
    @SneakyThrows
    public int compile(File inputFile) {
        javaIDE.insertLog(LogType.INFORMATION ,"Compiling class: " + inputFile.getName());
        javaIDE.insertToOutput("Compiling class: " + inputFile.getName(), true);
        int exitCode = 1;
        String errLogName = "err_compilation_" + inputFile.getName().toLowerCase() + "_" + Utils.getDate() + ".log";
        String outLogName = "out_compilation_" + inputFile.getName().toLowerCase() + "_" + Utils.getDate() + ".log";
        File errLog, outLog, classFile, pkgDir;
        String code = new String(Files.readAllBytes(inputFile.toPath()));
        String className = getClassName(code).replaceAll(" ", "");
        String packageName = getPackage(code);
        System.out.println(packageName);
        if(packageName.contains("."))
            packageName = packageName.replaceAll("\\.", Matcher.quoteReplacement(File.separator)) + Matcher.quoteReplacement(File.separator);
        pkgDir = new File(productionDir.getAbsolutePath() + Matcher.quoteReplacement(File.separator) + packageName + Matcher.quoteReplacement(File.separator));
        classFile = new File(pkgDir.getAbsolutePath() + Matcher.quoteReplacement(File.separator) + className + ".class");
        errLog = new File(logsDir, errLogName);
        javaIDE.insertLog(LogType.NORMAL ,"Location of error log: " + errLog.getAbsolutePath());
        javaIDE.insertToOutput("Location of error log: " + errLog.getAbsolutePath(), true);
        outLog = new File(logsDir, outLogName);
        javaIDE.insertLog(LogType.NORMAL ,"Location of general log: " + outLog.getAbsolutePath());
        javaIDE.insertToOutput("Location of general log: " + outLog.getAbsolutePath(), true);

        if(!pkgDir.exists())
            pkgDir.mkdirs();

        if(classFile.exists())
            classFile.delete();

        if(inputFile.getName().endsWith(".java")) {
            javaIDE.insertToOutput("Compiling java file: " + inputFile.getName(), true);
            exitCode = ToolProvider.getSystemJavaCompiler().run(null, new FileOutputStream(outLog), new FileOutputStream(errLog), inputFile.getAbsolutePath());
        } else {

        }
        lastError = errLog;
        javaIDE.insertToOutput("Compilation of class finished with exit code: " + exitCode, true);
        javaIDE.insertLog(LogType.NORMAL ,"Compilation of class finished with exit code: " + exitCode);
        if(exitCode == 0) {
            javaIDE.insertToOutput("Copying class file to production directory", true);
            //copy the compiled class to production directory
            File finalFile = new File(inputFile.getParentFile().getAbsolutePath() + Matcher.quoteReplacement(File.separator) + className + ".class");
            Files.copy(finalFile.toPath(), new File(pkgDir.getAbsolutePath() + Matcher.quoteReplacement(File.separator) + className + ".class").toPath());
            javaIDE.insertToOutput("Copying class file to production directory finished", true);
            javaIDE.insertLog(LogType.NORMAL ,"Copying class file to production directory finished");

            javaIDE.insertToOutput("Running checksum.. Comparing classes for integrity.", true);
            javaIDE.insertLog(LogType.NORMAL ,"Running checksum.. Comparing classes for integrity.");
            if(!checksum(classFile, MessageDigest.getInstance("MD5")).equals(checksum(finalFile, MessageDigest.getInstance("MD5")))) {
                javaIDE.insertToOutput("Checksum of class file and source file do not match", true);
                javaIDE.insertLog(LogType.ERROR ,"Checksum of class file and source file do not match");
                exitCode = 2;
            }
            javaIDE.insertToOutput("Checksum of class file and source file match", true);
            javaIDE.insertLog(LogType.NORMAL ,"Checksum of class file and source file match");
            finalFile.delete();
            javaIDE.insertToOutput("Copying class file to production directory finished", true);
        }

        javaIDE.insertToOutput("Process finished for class: " + className, true);
        javaIDE.insertLog(LogType.NORMAL ,"Process finished for class: " + className);
        javaIDE.insertToOutput("---------------------------------------------------------------", true);

        return exitCode;
    }

    @SneakyThrows
    public JarOutputStream repackDirectoryToJarFile(File directory, String artifactName){
        File jarFile = new File(outputDir.getAbsolutePath() + Matcher.quoteReplacement(File.separator) + artifactName + ".jar");
        if(jarFile.exists())
            jarFile.delete();

        javaIDE.insertToOutput("Repacking directory to jar file: " + artifactName, true);
        javaIDE.insertLog(LogType.NORMAL ,"Repacking directory to jar file: " + artifactName);
        Manifest manifest = new Manifest();

            javaIDE.insertToOutput("Creating manifest file", true);
            javaIDE.insertLog(LogType.NORMAL ,"Creating manifest file");
            javaIDE.insertToOutput("Adding attributes to manifest file", true);
            javaIDE.insertLog(LogType.NORMAL ,"Adding attributes to manifest file");

            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            javaIDE.insertToOutput("Manifest version set to: 1.0", true);
            javaIDE.insertLog(LogType.NORMAL ,"Manifest version set to: 1.0");

            if(artifactMainClass != null && artifactMainClass.length() > 0) {
                manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, artifactMainClass);
                javaIDE.insertToOutput("Manifest Main class set to: " + artifactMainClass, true);
                javaIDE.insertLog(LogType.NORMAL ,"Manifest Main class set to: " + artifactMainClass);
            }

            if(artifactName != null && artifactName.length() > 0) {
                manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_TITLE, artifactName);
                javaIDE.insertToOutput("Manifest Implementation title set to: " + artifactName, true);
                javaIDE.insertLog(LogType.NORMAL ,"Manifest Implementation title set to: " + artifactName);
            }

            if(artifactVersion != null && artifactVersion.length() > 0) {
                manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VERSION, artifactVersion);
                javaIDE.insertToOutput("Manifest Implementation version set to: " + artifactVersion, true);
                javaIDE.insertLog(LogType.NORMAL ,"Manifest Implementation version set to: " + artifactVersion);
            }

            if(artifactAuthor != null && artifactAuthor.length() > 0) {
                manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VENDOR, artifactAuthor);
                javaIDE.insertToOutput("Manifest Implementation vendor set to: " + artifactAuthor, true);
                javaIDE.insertLog(LogType.NORMAL ,"Manifest Implementation vendor set to: " + artifactAuthor);
            }


        javaIDE.insertToOutput("Creating jar file", true);
        javaIDE.insertLog(LogType.NORMAL ,"Creating jar file");

        javaIDE.insertToOutput("Adding manifest to jar file", true);
        javaIDE.insertLog(LogType.NORMAL ,"Adding manifest to jar file");
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile), manifest);
        javaIDE.insertToOutput("Manifest added to jar file", true);
        javaIDE.insertLog(LogType.NORMAL ,"Manifest added to jar file");

        javaIDE.insertToOutput("Building jar file", true);
        javaIDE.insertLog(LogType.NORMAL ,"Building jar file");
        processFolderToJar(directory, "", jos);

        javaIDE.insertToOutput("Jar file created: " + jarFile.getName(), true);
        javaIDE.insertLog(LogType.NORMAL ,"Jar file created: " + jarFile.getName());


        javaIDE.insertToOutput("Jar file built", true);
        javaIDE.insertLog(LogType.NORMAL ,"Jar file built");
        javaIDE.insertToOutput("---------------------------------------------------------------\n", true);
        javaIDE.insertLog(LogType.NORMAL ,"---------------------------------------------------------------");
        javaIDE.insertToOutput("BUILD OF " + artifactName +  " FINISHED AT " + Utils.getDate() + "\n", true);
        javaIDE.insertLog(LogType.NORMAL ,"BUILD OF " + artifactName +  " FINISHED AT " + Utils.getDate());
        javaIDE.insertToOutput("---------------------------------------------------------------\n", true);
        javaIDE.insertLog(LogType.NORMAL ,"---------------------------------------------------------------");
        return jos;
    }

    public void processFolderToJar(File directory, String path, JarOutputStream jar) {
        javaIDE.insertToOutput("Processing folder: " + directory.getName(), true);
        javaIDE.insertLog(LogType.NORMAL ,"Processing folder: " + directory.getName());
        for(File file : directory.listFiles()) {
            javaIDE.insertToOutput("Processing file: " + file.getName(), true);
            javaIDE.insertLog(LogType.NORMAL ,"Processing file: " + file.getName());
            if(file.isFile())
                addFileToJar(file, path, jar);
            if(file.isDirectory())
                processFolderToJar(file, path + file.getName() + "/", jar);
        }

    }



    public void addFileToJar(File file, String path, JarOutputStream jar) {
        javaIDE.insertToOutput("Adding file: " + file.getName(), true);
        javaIDE.insertLog(LogType.NORMAL ,"Adding file: " + file.getName());
        try {
            jar.putNextEntry(new JarEntry(path + file.getName()));
            javaIDE.insertToOutput("File added: " + file.getName(), true);
            javaIDE.insertLog(LogType.NORMAL ,"File added: " + file.getName());
            byte[] buffer = new byte[1024];
            FileInputStream fis = new FileInputStream(file);
            int length;
            while((length = fis.read(buffer)) > 0) {
                jar.write(buffer, 0, length);
            }
            fis.close();
            jar.closeEntry();
        } catch (Exception e) {
            javaIDE.insertToOutput("Error adding file: " + file.getName(), true);
            javaIDE.insertLog(LogType.ERROR ,"Error adding file: " + file.getName());
        }
    }


    public static String getClassName(String code){
        String c = "";
        try {
            c = code.substring(code.indexOf("class") + 6, code.indexOf("{"));
        } catch (Exception e) {
            return "";
        }
        return c;
    }

    public static String getPackage(String code){
        String p = "";
        try {
            p = code.substring(code.indexOf("package") + 8, code.indexOf(";"));
        } catch (Exception e) {
            return "";
        }
        return p;
    }

    @SneakyThrows
    public String checksum(File file, MessageDigest digest) {
        javaIDE.insertToOutput("Calculating checksum of file: " + file.getName(), true);
        javaIDE.insertLog(LogType.NORMAL ,"Calculating checksum of file: " + file.getName());
        FileInputStream fis = new FileInputStream(file);
        byte[] byteArray = new byte[1024];
        int bytesCount = 0;
        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        };
        fis.close();
        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for(int i=0; i< bytes.length ;i++) {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        javaIDE.insertToOutput("Checksum calculated: " + sb.toString(), true);
        javaIDE.insertLog(LogType.NORMAL ,"Checksum calculated: " + sb.toString());
        return sb.toString();
    }

}


