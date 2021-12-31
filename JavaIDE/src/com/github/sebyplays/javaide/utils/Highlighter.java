package com.github.sebyplays.javaide.utils;

import lombok.SneakyThrows;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.HashMap;

public class Highlighter {
    public HashMap<String, DefaultHighlighter.DefaultHighlightPainter> highlightedWords = new HashMap<>();

    public DefaultHighlighter.DefaultHighlightPainter highlightPainter;


    public void add(String word, Color color) {
        if(!highlightedWords.containsKey(word)) {
            highlightedWords.put(word, new DefaultHighlighter.DefaultHighlightPainter(color));
        }
    }

    public void remove(String word) {
        if(highlightedWords.containsKey(word)) {
            highlightedWords.remove(word);
        }
    }

    public Color getColor(String word) {
        if(highlightedWords.containsKey(word)) {
            return highlightedWords.get(word).getColor();
        }
        return null;
    }

    public void add(String word, int r, int g, int b) {
        add(word, new Color(r, g, b));
    }

    @SneakyThrows
    public void applyHighlighter(JTextComponent textComponent) {
        //apply text highlights
        for(String word : highlightedWords.keySet()) {
            highlightPainter = highlightedWords.get(word);
            javax.swing.text.Highlighter highlighter = textComponent.getHighlighter();
            javax.swing.text.Document doc = textComponent.getDocument();
            String text = doc.getText(0, doc.getLength());
            int pos = 0;
            while((pos = text.indexOf(word, pos)) != -1) {
                highlighter.addHighlight(pos, pos + word.length(), highlightPainter);
                pos += word.length();
            }
        }
    }

}
