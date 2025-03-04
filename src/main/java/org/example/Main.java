package org.example;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        TextProcessing textProcessing = new TextProcessing("Hello World");
        textProcessing.splitWords();
        BabelNetApiClient babelNetApiClient = new BabelNetApiClient(textProcessing.getWordList());
        ConceptNetApiClient conceptNetApiClient = new ConceptNetApiClient(textProcessing.getWordList());
    }
}