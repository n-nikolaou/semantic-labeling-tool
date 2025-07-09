package org.example.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.stanford.nlp.ling.CoreLabel;

import java.util.*;

public class IndexedWordModel {
    public Integer index;
    public String word, lemma, posTag;
    @JsonProperty(required = false)
    public String ner;
    @JsonProperty(required = false)
    public ArrayList<GrammaticalRelation> relations;
    @JsonIgnore()
    public CoreLabel token;
    @JsonProperty(required = false)
    public Integer chainMentionId;
    @JsonProperty(required = false)
    public Integer mentionId;
    @JsonIgnore()
    public Quotation quotation;

    public IndexedWordModel(CoreLabel token) {
        this.token = token;
    }

    public IndexedWordModel() {}

    public static class GrammaticalRelation {
        public List<Integer> targetIndices = new ArrayList<>();
        public String grammaticalRelation;

        public GrammaticalRelation(Integer targetIndex, String grammaticalRelation) {
            this.targetIndices.add(targetIndex);
            this.grammaticalRelation = grammaticalRelation;
        }

        public void addTargetIndex(int targetIndex) {
            targetIndices.add(targetIndex);
        }

        @Override
        public String toString() {
            return "Target Indices: " + targetIndices.toString() + ", Relation: " + grammaticalRelation;
        }

        @Override
        public int hashCode() {
            return Objects.hash(grammaticalRelation, targetIndices);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Index: ").append(index).append("\n");
        sb.append("Word: ").append(word).append("\n");
        sb.append("Lemma: ").append(lemma).append("\n");
        sb.append("POS Tag: ").append(posTag).append("\n");

        if (ner != null) {
            sb.append("NER: ").append(ner).append("\n");
        }

        if (relations != null && !relations.isEmpty()) {
            sb.append("Grammatical Relations:\n");
            for (GrammaticalRelation rel : relations) {
                sb.append("  - Target Index: ").append(rel.targetIndices.toString())
                        .append(", Relation: ").append(rel.grammaticalRelation).append("\n");
            }
        }

        return sb.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, word, lemma, posTag, ner,
                relations);
    }
}
