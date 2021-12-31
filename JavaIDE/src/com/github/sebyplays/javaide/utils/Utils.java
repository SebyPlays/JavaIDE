package com.github.sebyplays.javaide.utils;

import com.strobel.decompiler.Decompiler;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import lombok.SneakyThrows;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {

    public static String getDate(){
        return new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss").format(new Date());
    }


    //TODO: add a method that decompiles the bytecode and returns a string with the decompiled code


    public static String decompile(JEditorPane jep, File file){
        StringWriter writer = new StringWriter();
        try{

            jep.setText("Loading...");
            // put writer contents into a string
            Decompiler.decompile(
                    file.getAbsolutePath(),
                    new PlainTextOutput(writer),
                    DecompilerSettings.javaDefaults());
        } catch (Exception e){
            jep.setText("Error: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
        return writer.toString();
    }


    public static void openDirectory(String directory){
        try {
            Desktop.getDesktop().open(new File(directory));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean equalsIgnoreMultiple(String input, String... choices){
        for(String s : choices){
            if(input.equalsIgnoreCase(s)){
                return true;
            }
        }
        return false;
    }

    @SneakyThrows
    public static void invokeMethod(File file, String classpath, String methodName, Class[] param, Object... args){
        Class jClass = Class.forName(classpath, true, new URLClassLoader(new URL[]{file.toURI().toURL()}, Utils.class.getClass().getClassLoader()));
        Method method = jClass.getDeclaredMethod(methodName, param);

        method.invoke(jClass.newInstance(), (args));
    }

    @SneakyThrows
    public static void invokeMain(File file, String classpath, String[] args){
        Class jClass = Class.forName(classpath, true, new URLClassLoader(new URL[]{file.toURI().toURL()}, Utils.class.getClass().getClassLoader()));
        Method method = jClass.getDeclaredMethod("main", String[].class);
        final Object[] argsObj = new Object[1];
        argsObj[0] = args;
        method.invoke(null, argsObj);
    }

}
