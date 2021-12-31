package com.github.sebyplays.javaide.utils;

import java.util.HashMap;
import java.util.Locale;

public class AutoComplete {

    public HashMap<String, String> autocomplete = new HashMap<>();

    public AutoComplete(){

    }

    public void add(String key, String code){
        if(!autocomplete.containsKey(key))
            autocomplete.put(key, code);
    }

    public void remove(String key){
     if(autocomplete.containsKey(key))
        autocomplete.remove(key);
    }

    public String processRequest(String request){
        if(request == null)
            return "";
        if(autocomplete.containsKey(request.toLowerCase()))
            return autocomplete.get(request.toLowerCase());
        return "";
    }

}
