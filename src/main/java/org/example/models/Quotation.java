package org.example.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.stanford.nlp.ling.CoreLabel;

import java.util.ArrayList;
import java.util.List;

public class Quotation {
    @JsonProperty("id")
    public int id;
    @JsonProperty("speaker")
    public String speaker;
    @JsonIgnore()
    public String quote;
    @JsonIgnore()
    public List<CoreLabel> tokens;
    @JsonIgnore()
    public List<IndexedWordModel> indexedWords;
    @JsonProperty("indices")
    public List<Integer> indices;

    public Quotation(int id, String quote, String speaker, List<CoreLabel> tokens) {
        this.id = id;
        this.quote = quote;
        this.speaker = speaker;
        this.tokens = tokens;
        indexedWords = new ArrayList<>();
    }

    public Quotation() {}

    public boolean isTokenInsideQuotation(CoreLabel token) {
        return !tokens.stream()
                .filter(coreLabel -> coreLabel.beginPosition() == token.beginPosition())
                .toList()
                .isEmpty();
    }

    public void addIndexedWord(IndexedWordModel word) {
        indexedWords.add(word);
    }
}
