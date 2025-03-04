package org.example;

import java.util.ArrayList;
import java.util.stream.Stream;

public class TextProcessing {
    private final String inputText;
    private ArrayList<String> wordList;

    TextProcessing(String inputText) {
        this.inputText = inputText;
        splitWords();
    }

    void splitWords() {
        wordList = new ArrayList<>(Stream.of(inputText.split("\\W+")).map(String::toLowerCase).toList());
    }

    String getInputText() {
        return inputText;
    }

    ArrayList<String> getWordList() {
        return wordList;
    }
}
