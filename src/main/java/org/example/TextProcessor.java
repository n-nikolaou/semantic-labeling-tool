package org.example;

import io.github.semlink.verbnet.VnIndex;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.lemmatizer.DictionaryLemmatizer;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagFormat;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class TextProcessor {
    Set<String> loc = new HashSet<>(Arrays.asList("in", "on", "at", "under", "over", "by", "beside", "near", "above", "below", "inside", "outside", "between", "among", "around"));
    Set<String> path = new HashSet<>(Arrays.asList("through", "across", "along", "past", "over", "under", "around", "between", "beyond", "down", "up", "via", "throughout"));
    Set<String> src = new HashSet<>(Arrays.asList("from", "out of", "off", "away from", "out"));
    Set<String> dir = new HashSet<>(Arrays.asList("toward", "to", "into", "onto", "upward", "downward", "forward", "backward", "along", "across", "toward the north"));
    Set<String> dest = new HashSet<>(Arrays.asList("to", "into", "onto", "toward", "onto the table", "into the house"));
    Set<String> destDir = new HashSet<>(Arrays.asList("toward", "in the direction of", "headed to", "moving toward", "advancing to", "walking into"));
    Set<String> destConf = new HashSet<>(Arrays.asList("into a box", "onto the table", "inside the container", "under the bed", "neatly on the shelf"));
    Set<String> spatial = new HashSet<>();
    HashMap<String, Set> typeToSets = new HashMap<>();

    private String[] stringTokens;
    private String[] customTags;
    private String[] lemmas;
    private String[] tags;
    private String[] generatedChunks;
    private VnIndex vnIndex;

    private String[] generateLemmas() throws IOException {
        InputStream dictLemmatizer = getClass()
                .getResourceAsStream("/models/en-lemmatizer.dict");
        DictionaryLemmatizer lemmatizer = new DictionaryLemmatizer(
                dictLemmatizer);

        String[] lemmas = Arrays.stream(lemmatizer.lemmatize(stringTokens, customTags))
                .map(lemma -> Objects.equals(lemma, "O") ? null : lemma)
                .toArray(String[]::new);

        for (int i = 0; i < lemmas.length; i++) {
            lemmas[i] = (lemmas[i] == null && (Objects.equals(tags[i], "NOUN") || Objects.equals(tags[i], "VERB")))
                ? stringTokens[i].toLowerCase()
                : lemmas[i];
        }
        return lemmas;
    }

    private String[] generateTags(boolean isCustomFormat) throws IOException {
        InputStream inputStreamPOSTagger = getClass()
                .getResourceAsStream("/models/en-pos-maxent.bin");
        POSModel posModel = new POSModel(inputStreamPOSTagger);
        POSTaggerME posTagger = new POSTaggerME(posModel, isCustomFormat ? POSTagFormat.CUSTOM : POSTagFormat.UNKNOWN);

        String tags[] = Arrays.stream(posTagger.tag(stringTokens))
                .map(tag -> (Objects.equals(tag, "PROPN") || Objects.equals(tag, "PRON"))
                        ? "NOUN"
                        : (Objects.equals(tag, "DET") || Objects.equals(tag, "PART") || Objects.equals(tag, "PUNCT"))
                            ? null
                            : tag)

                .toArray(String[]::new);
        return tags;
    }

    private String[] generateTokens(String sentence) {
        SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
        String[] tokens = tokenizer.tokenize(sentence);
        return tokens;
    }

    private String[] generateChunks() throws IOException {
        InputStream inputStreamChunker = getClass()
                .getResourceAsStream("/models/en-chunker.bin");
        ChunkerModel chunkerModel
                = new ChunkerModel(inputStreamChunker);
        ChunkerME chunker = new ChunkerME(chunkerModel);
        return chunker.chunk(stringTokens, customTags);
    }


    public ArrayList<String> getStringTokens() {
        return (ArrayList<String>) List.of(stringTokens);
    }

    public String[] getCustomTags() {
        return customTags;
    }

    public String[] getTags() {
        return tags;
    }

    public String[] getLemmas() {
        return lemmas;
    }

    public String[] getGeneratedChunks() {
        return generatedChunks;
    }

}
