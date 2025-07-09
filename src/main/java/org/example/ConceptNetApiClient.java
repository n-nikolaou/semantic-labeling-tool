package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.models.Edge;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

public class ConceptNetApiClient extends SemanticApiClient {
    List<String> posTags;

    ConceptNetApiClient(Adapter adapter) {
        super(adapter);
        posTags = words.stream().map(word -> word.posTag).collect(Collectors.toList());
    }

    HashMap<String, List<Edge>> getEdges() {
        List<Map<String, List<Edge>>> edgesPerLemmaList = lemmas.stream()
                .filter(lemma -> lemma.matches("[a-zA-Z]+"))
                .map(lemma -> {
                    String url = getApiUrl(lemma);

                    HttpResponse<String> response = sendHttpRequest(url);
                    JSONObject obj = (JSONObject) JSONValue.parse(response.body());
                    String jsonArray = JSONValue.toJSONString(obj.get("edges"));

                    ObjectMapper mapper = new ObjectMapper();

                    try {
                        return Map.of(lemma, mapper.readValue(
                                jsonArray,
                                new TypeReference<List<Edge>>() {}
                        ));
                    } catch (Exception e) {
                        return new HashMap<String, List<Edge>>();
                    }
                })
                .toList();

        HashMap<String, List<Edge>> edgesPerLemma = new HashMap<>();
        for (Map<String, List<Edge>> stringListMap : edgesPerLemmaList) {
            edgesPerLemma.putAll(stringListMap);
        }


        return edgesPerLemma;
    }

    private String getUriId(JSONObject jsonObject, String POSTag) {
        if (POSTag != null) {
            return jsonObject.get("@id").toString();
        }
        return jsonObject.get("id").toString();
    }

    @Override
    protected String getApiUrl(String word) {
        return "https://api.conceptnet.io/c/en/%s?limit=10".formatted(word);
    }
}
