package org.example.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.stanford.nlp.ling.CoreLabel;

import java.util.Arrays;
import java.util.Objects;

public class IndexedWordModel {
    public Integer index;
    public String word, lemma, posTag;
    @JsonProperty(required = false)
    public String ner;
    @JsonProperty(required = false)
    public GrammaticalRelation[] relations;
    @JsonProperty(required = false)
    public VerbDetails verbDetails;
    @JsonIgnore()
    public CoreLabel token;

    public IndexedWordModel(CoreLabel token) {
        this.token = token;
    }

    public static class GrammaticalRelation {
        public int[] targetIndices;
        public String grammaticalRelation;

        public GrammaticalRelation(Integer targetIndex, String grammaticalRelation) {
            this.targetIndices = new int[]{targetIndex};
            this.grammaticalRelation = grammaticalRelation;
        }

        public GrammaticalRelation(Integer[] targetIndices, String grammaticalRelation) {
            this.targetIndices = Arrays.copyOf(
                    Arrays.stream(targetIndices)
                        .mapToInt(i -> i != null ? i : -1)
                        .toArray(),
                    targetIndices.length);
            this.grammaticalRelation = grammaticalRelation;
        }

        @Override
        public String toString() {
            return "Target Indices: " + Arrays.toString(targetIndices) + ", Relation: " + grammaticalRelation;
        }

        @Override
        public int hashCode() {
            return Objects.hash(grammaticalRelation, Arrays.hashCode(targetIndices));
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

        if (relations != null && relations.length > 0) {
            sb.append("Grammatical Relations:\n");
            for (GrammaticalRelation rel : relations) {
                sb.append("  - Target Index: ").append(Arrays.toString(rel.targetIndices))
                        .append(", Relation: ").append(rel.grammaticalRelation).append("\n");
            }
        }

        if (verbDetails != null) {
            sb.append("Verb Details:\n").append(verbDetails.toString().indent(2));
        }

        return sb.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, word, lemma, posTag, ner,
                Arrays.hashCode(relations),
                verbDetails);
    }
}
