package com.github.sebyplays.javaide.utils;

import com.github.sebyplays.logmanager.api.LogManager;
import com.github.sebyplays.logmanager.api.LogType;
import lombok.SneakyThrows;

import java.io.IOException;

public class Logger {

    private String name;
    private String prefix;
    private boolean print = true;
    private boolean printDefaultValue = true;

    public LogManager logManager;

    public Logger(String name) throws IOException {
        this.name = name;
        logManager = LogManager.getLogManager(name);
    }

    public Logger(String name, boolean print){
        this.name = name;
        this.printDefaultValue = print;
        this.print = printDefaultValue;
    }

    public Logger name(String name){
        this.prefix = name;
        return this;
    }

    public Logger print(boolean print){
        this.print = print;
        return this;
    }

    @SneakyThrows
    public void info(String msg){
        LogManager.getLogManager(name).log(LogType.INFORMATION, prefix != null ? "[" + prefix + "] " + msg : msg, true, false, true, print);
        prefix = null;
        print = printDefaultValue;
    }
    @SneakyThrows
    public void warning(String msg){
        LogManager.getLogManager(name).log(LogType.WARNING, prefix != null ? "[" + prefix + "] " + msg : msg, true, false, true, print);
        prefix = null;
        print = printDefaultValue;

    }
    @SneakyThrows
    public void error(String msg){
        LogManager.getLogManager(name).log(LogType.ERROR, prefix != null ? "[" + prefix + "] " + msg : msg, true, false, true, print);
        prefix = null;
        print = printDefaultValue;
    }
    @SneakyThrows
    public void normal(String msg){
        LogManager.getLogManager(name).log(LogType.NORMAL, prefix != null ? "[" + prefix + "] " + msg : msg, true, false, true, print);
        prefix = null;
        print = printDefaultValue;

    }

}
