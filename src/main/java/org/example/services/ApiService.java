package org.example.services;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.example.GraphDBMapper;
import org.example.Main;
import org.example.Adapter;
import org.example.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ApiService {
    private final RepositoryConnection connection;
    private final Map<String, String> jobResults = new ConcurrentHashMap<>();

    @Autowired
    public ApiService(RepositoryConnection connection) {
        this.connection = connection;
    }

    public ArrayList<IndexedWordModel> getIndexedWords() {
        String queryString =
                """
                PREFIX ex: <http://example.org/>
                SELECT ?index ?lemma ?partOfSpeech ?word ?ner ?chainMention ?mention
                WHERE {
                  ?token a ex:Token .
                  ?token ex:hasIndex ?index .
                  ?token ex:hasLemma ?lemma .
                  ?token ex:hasPartOfSpeech ?partOfSpeech .
                  ?token ex:word ?word .
                  OPTIONAL {
                    ?token ex:hasNamedEntityRecognition ?ner .
                  }
                  OPTIONAL {
                    ?token ex:hasChainMention ?chainMention .
                    ?token ex:hasMention ?mention .
                  }
                } ORDER BY ?token
                """;

        TupleQuery query = connection.prepareTupleQuery(queryString);

        ArrayList<IndexedWordModel> words = new ArrayList<>();
        try (TupleQueryResult result = query.evaluate()) {
            for (BindingSet solution : result) {
                if (solution != null) {
                    IndexedWordModel indexedWordModel = new IndexedWordModel();
                    indexedWordModel.index = Integer.parseInt(solution.getValue("index").stringValue());;
                    indexedWordModel.lemma = solution.getValue("lemma").stringValue();
                    indexedWordModel.word = solution.getValue("word").stringValue();
                    indexedWordModel.posTag = solution.getValue("partOfSpeech").stringValue();

                    if (solution.getValue("ner") != null) {
                        indexedWordModel.ner = solution.getValue("ner").stringValue();
                    }

                    if (solution.getValue("chainMention") != null) {
                        indexedWordModel.chainMentionId = Integer.parseInt(solution.getValue("chainMention").stringValue());
                        indexedWordModel.mentionId = Integer.parseInt(solution.getValue("mention").stringValue());
                    }

                    words.add(indexedWordModel);
                }
            }
        }

        return words;
    }

    public ArrayList<IndexedWordModel.GrammaticalRelation> getGrammaticalRelations(int sourceIndex) {
        String queryString =
                """
                PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX ex: <http://example.org/>
                select ?targetIndex ?relation
                where {
                    ?token a ex:Token .
                    ?token ex:hasIndex ?index .
                    ?token ex:hasGrammaticalRelation ?grammaticalRelation .
                    ?grammaticalRelation ex:target ?targetToken .
                    ?targetToken ex:hasIndex ?targetIndex .
                    ?grammaticalRelation ex:relation ?relation .""" +
                    "FILTER(?index = " + sourceIndex + ")" +
                "}";

        TupleQuery query = connection.prepareTupleQuery(queryString);

        ArrayList<IndexedWordModel.GrammaticalRelation> relations = new ArrayList<>();
        try (TupleQueryResult result = query.evaluate()) {
            for (BindingSet solution : result) {
                if (solution != null) {
                    String relation = solution.getValue("relation").stringValue();
                    int targetIndex = Integer.parseInt(solution.getValue("targetIndex").stringValue());
                    IndexedWordModel.GrammaticalRelation grammaticalRelation = new IndexedWordModel.GrammaticalRelation(targetIndex, relation);
                    relations.add(grammaticalRelation);
                }
            }
        }

        return relations;
    }

    public ArrayList<ArrayList<Integer>> getRelationTriples(int targetIndex) {
        String queryString =
                """
                PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX ex: <http://example.org/>
                select ?index ?r
                where {
                    ?token ex:hasIndex ?index .
                    ?token ex:isInRelationTriple ?r .
                } order by ?r""";

        TupleQuery query = connection.prepareTupleQuery(queryString);
        String relation = null;
        ArrayList<Integer> wordSequence = new ArrayList<>();

        ArrayList<ArrayList<Integer>> wordSequences = new ArrayList<>();
        try (TupleQueryResult result = query.evaluate()) {
            for (BindingSet solution : result) {
                if (solution != null) {
                    if (!Objects.equals(relation, solution.getValue("r").stringValue())) {
                        relation = solution.getValue("r").stringValue();
                        if (!wordSequence.isEmpty()) {
                            wordSequences.add(wordSequence);
                        }
                        wordSequence = new ArrayList<>();
                    }

                    wordSequence.add(Integer.parseInt(solution.getValue("index").stringValue()));
                }
            }
        }

        if (!wordSequence.isEmpty()) {
            wordSequences.add(wordSequence);
        }

        return wordSequences;
    }

    public List<Quotation> getQuotations() {
        String queryString =
                """
                PREFIX ex: <http://example.org/>
                select ?quotation ?speaker ?word ?index
                where {
                    ?quotation a ex:Quotation .
                    optional {
                        ?quotation ex:hasSpeaker ?speaker.
                    }
                    ?quotation ex:hasText ?text .
                    ?quotation ex:hasToken ?token .
                    ?token ex:word ?word.
                    ?token ex:hasIndex ?index .
                } order by ?quotation ?index
                """;

        TupleQuery query = connection.prepareTupleQuery(queryString);
        String quotationNamespace = null;
        Quotation quotation = null;

        List<Quotation> quotations = new ArrayList<>();
        try (TupleQueryResult result = query.evaluate()) {
            for (BindingSet solution : result) {
                if (solution != null) {
                    if (!Objects.equals(quotationNamespace, solution.getValue("quotation").stringValue())) {
                        quotationNamespace = solution.getValue("quotation").stringValue();

                        if (quotation != null) {
                            quotations.add(quotation);
                        }

                        quotation = new Quotation();
                        quotation.speaker = solution.getValue("speaker") == null
                                ? null
                                : solution.getValue("speaker").stringValue();
                        quotation.indices = new ArrayList<>();
                    }

                    quotation.indices.add(Integer.parseInt(solution.getValue("index").stringValue()));
                }
            }
        }

        if (quotationNamespace != null) {
            quotations.add(quotation);
        }

        return quotations;
    }

    public List<Edge> getConceptNetEdges(int targetIndex) {
        String queryString = """
                PREFIX ex: <http://example.org/>
                select ?startLabel ?relation ?endLabel ?relationWeight
                where {
                    ?token a ex:Token .
                    ?token ex:hasIndex ?index.
                    ?token ex:hasLemma ?lemma .
                    ?edge a ex:Edge.
                    ?edge ex:hasStart ?nodeS.
                    ?edge ex:hasEnd ?nodeE.
                    ?edge ex:forLemma ?lemma .
                    ?edge ex:hasRelationWeight ?relationWeight.
                    ?nodeS ex:hasLabel ?startLabel .
                    ?nodeE ex:hasLabel ?endLabel .
                    ?nodeS ?relation ?nodeE .""" +
                    "FILTER(?index = " + targetIndex + " && ?relation != ex:relatedToCN)\n"+
                "} ORDER BY ?startLabel ?relation";

        TupleQuery query = connection.prepareTupleQuery(queryString);
        List<Edge> relations = new ArrayList<>();

        try (TupleQueryResult result = query.evaluate()) {
            for (BindingSet solution : result) {
                if (solution != null) {
                    Edge edge = new Edge();
                    Edge.Node start = new Edge.Node();
                    Edge.Node end = new Edge.Node();
                    Edge.Relation relation = new Edge.Relation();

                    start.label = solution.getValue("startLabel").stringValue();
                    end.label = solution.getValue("endLabel").stringValue();
                    relation.label = solution.getValue("relation").stringValue();

                    edge.start = start;
                    edge.end = end;
                    edge.relation = relation;
                    edge.weight = Double.parseDouble(solution.getValue("relationWeight").stringValue());
                    relations.add(edge);
                }
            }
        }

        return relations;
    }

    public List<String> getBabelNetRelations(int targetIndex) {
        String queryString = """
                PREFIX ex: <http://example.org/>
                select ?relWord
                where {
                    ?token a ex:Token .
                    ?token ex:hasIndex ?index .
                    ?token ex:hasRelevantWord ?relWord .""" +
                    "FILTER(?index = " + targetIndex + ")\n" +
                "}";

        TupleQuery query = connection.prepareTupleQuery(queryString);
        List<String> relevantWords = new ArrayList<>();

        try (TupleQueryResult result = query.evaluate()) {
            for (BindingSet solution : result) {
                if (solution != null) {
                    relevantWords.add(solution.getValue("relWord").stringValue());
                }
            }
        }

        return relevantWords;
    }

    @Async
    public void processAsync(String text, String jobId) {
        Adapter adapter = Main.initialize(text);
        adapter.mapWordsToModels();

        String result = "ANNOTATED TEXT";
        jobResults.put(jobId, result);

        new GraphDBMapper(adapter);

        result = "MAPPED TEXT TO GRAPH";
        jobResults.put(jobId, result);

    }

    public String getResult(String jobId) {
        return jobResults.get(jobId);
    }
}
