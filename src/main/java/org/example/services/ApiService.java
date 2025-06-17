package org.example.services;

import org.apache.http.client.fluent.Response;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.example.GraphDBMapper;
import org.example.VerbNetAdapter;
import org.example.models.IndexedWordModel;
import org.example.models.SemanticArgument;
import org.example.models.VerbDetails;
import org.json.simple.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.example.models.IndexedWordModel.IndexedWords.findIndexedWordsByIndex;

@Service
public class ApiService {
    private final RepositoryConnection connection;
    private final Map<String, String> jobResults = new ConcurrentHashMap<>();

    @Autowired
    public ApiService(RepositoryConnection connection) {
        this.connection = connection;
    }

    public ArrayList<IndexedWordModel> getIndexedWords() {
        System.out.println("getIndexedWords");
        String queryString =
                """
                PREFIX ex: <http://example.org/>
                SELECT ?index ?lemma ?partOfSpeech ?word ?ner
                WHERE {
                  ?token a ex:Token .
                  ?token ex:hasIndex ?index .
                  ?token ex:hasLemma ?lemma .
                  ?token ex:hasPartOfSpeech ?partOfSpeech .
                  ?token ex:word ?word .
                  OPTIONAL {
                    ?token ex:hasNamedEntityRecognition ?ner .
                  }
                }
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

    public IndexedWordModel getVerbDetails(IndexedWordModel verb) {
        if (verb.ner.contains("VB") && verb.index != null && verb.index > -1) {
            String queryString = """
                    PREFIX ex: <http://example.org/>
                    SELECT *
                    WHERE {
                        ?token a ex:Verb .
                        ?token ex:hasIndex ?index .
                        OPTIONAL {
                       		?token ex:hasThematicRole ?themRole .
                            ?themRole ex:thematicType ?themType .
                            ?themRole ex:token ?themToken .
                            ?themToken ex:hasIndex ?themIndex .
                        }
                        ?token ex:hasPredicate ?predicate .
                        ?predicate ex:value ?value .
                        ?predicate ex:argument ?semArg .
                        ?semArg ex:argumentValue ?argValue .
                        ?semArg ex:argumentType ?argType.
                        FILTER(?index = """ + verb.index + ")\n"+
                    "} ORDER BY ?predicate ?semArg";

            TupleQuery query = connection.prepareTupleQuery(queryString);

            ArrayList<VerbDetails.ThematicRole> thematicRoles = new ArrayList<>();
            HashMap<String, SemanticArgument> semArgs = new HashMap<>();
            HashMap<String, ArrayList<SemanticArgument.Argument>> semArgsPerPredicate = new HashMap<>();

            String currentPredicateLocalName = "", currentArgumentLocalName = "";
            SemanticArgument currentPredicate = null;
            ArrayList<SemanticArgument.Argument> currentArguments = null;
            ArrayList<SemanticArgument> predicates = new ArrayList<>();

            try (TupleQueryResult result = query.evaluate()) {
                for (BindingSet solution : result) {
                    if (solution != null) {
                        Integer indexThemToken = (solution.getValue("themIndex") != null)
                            ? Integer.parseInt(solution.getValue("themIndex").stringValue())
                            : null;

                        if (
                                indexThemToken != null &&
                                !thematicRoles.stream().map(role -> role.wordIndex).toList().contains(indexThemToken)
                        ) {
                            VerbDetails.ThematicRole thematicRole = new VerbDetails.ThematicRole();
                            thematicRole.wordIndex = indexThemToken;
                            thematicRole.type = solution.getValue("themType").stringValue();
                            thematicRoles.add(thematicRole);
                        }

                        if (
                                Objects.equals(currentArgumentLocalName, "") ||
                                        !Objects.equals(currentArgumentLocalName, solution.getValue("semArg").stringValue())
                        ) {
                            if (currentArguments != null) {
                                currentPredicate.arguments = currentArguments.toArray(SemanticArgument.Argument[]::new);
                            }

                            if (
                                Objects.equals(currentPredicateLocalName, "") ||
                                !Objects.equals(currentPredicateLocalName, solution.getValue("predicate").stringValue())
                            ) {
                                currentArguments = new ArrayList<>();
                            }

                            currentArgumentLocalName = solution.getValue("semArg").stringValue();
                            SemanticArgument.Argument currentArgument = new SemanticArgument.Argument();

                            currentArgument.type = solution.getValue("argType").stringValue();
                            currentArgument.value = solution.getValue("argValue").stringValue();

                            currentArguments.add(currentArgument);
                        }

                        if (
                                Objects.equals(currentPredicateLocalName, "") ||
                                !Objects.equals(currentPredicateLocalName, solution.getValue("predicate").stringValue())
                        ) {
                            if (currentPredicate != null) {
                                predicates.add(currentPredicate);
                            }

                            currentPredicateLocalName = solution.getValue("predicate").stringValue();
                            currentPredicate = new SemanticArgument();

                            currentPredicate.predicate = solution.getValue("value").stringValue();
                        }
                    }
                }
            }

            if (currentPredicate != null) {
                currentPredicate.arguments = currentArguments.toArray(SemanticArgument.Argument[]::new);
                predicates.add(currentPredicate);
            }

            verb.verbDetails = new VerbDetails();
            verb.verbDetails.thematicRoles = thematicRoles.toArray(VerbDetails.ThematicRole[]::new);
            verb.verbDetails.semanticArguments = predicates.toArray(SemanticArgument[]::new);
        }

        return verb;
    }

    @Async
    public void processAsync(String text, String jobId) {
        VerbNetAdapter adapter = new VerbNetAdapter(text);
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
