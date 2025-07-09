package org.example.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Edge {
    @JsonProperty("@id")
    public String id;
    @JsonProperty("start")
    public Node start;
    @JsonProperty("end")
    public Node end;
    @JsonProperty("surfaceText")
    public String surfaceText;
    @JsonProperty("weight")
    public double weight;
    @JsonProperty("rel")
    public Relation relation;

    @Override
    public String toString() {
        return "Edge{" +
                "id='" + id + '\'' +
                ", end=" + end +
                ", start=" + start +
                ", surfaceText='" + surfaceText + '\'' +
                ", weight=" + weight +
                '}';
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Node {
        @JsonProperty("@id")
        public String id;
        @JsonProperty("label")
        public String label;
        @JsonProperty("language")
        public String language;

        @Override
        public String toString() {
            return "Node{" +
                    "id='" + id + '\'' +
                    ", language='" + language + '\'' +
                    ", lemma='" + label + '\'' +
                    '}';
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Relation {
        @JsonProperty("@id")
        public String id;
        @JsonProperty("label")
        public String label;
    }
}