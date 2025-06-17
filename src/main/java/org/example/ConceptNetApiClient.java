package org.example;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class ConceptNetApiClient extends SemanticApiClient {
    List<String> posTags;

    ConceptNetApiClient(VerbNetAdapter verbNetAdapter) throws ExecutionException, InterruptedException {
        super(verbNetAdapter);
        urls = lemmas.stream().map(this::getApiUrl).collect(Collectors.toList());
        posTags = words.stream().map(word -> word.posTag).collect(Collectors.toList());

        List<HttpResponse<String>> responses = sendHttpRequests(urls);
        JSONObject[] jsonObjects = responses.stream().map(response -> JSONValue.parse(response.body())).toArray(JSONObject[]::new);

//        ListIterator<JSONObject> iterator = List.of(jsonObjects).listIterator();
//        wordsId = new ArrayList<>(posTags.size());
//        while (iterator.hasNext()) {
//            int index = iterator.nextIndex();
//            wordsId.add(getUriId(jsonObjects[index], posTags.get(index)));
//        }
//
//        for (int i = 0; i < wordsId.size(); i++) {
//            System.out.println(wordsId.get(i) + " " + lemmas.get(i));
//        }
    }

    private String getUriId(JSONObject jsonObject, String POSTag) {
        if (POSTag != null) {
            return jsonObject.get("@id").toString();
        }
        return jsonObject.get("id").toString();
    }

    @Override
    protected String getApiUrl(String word) {
        return "https://api.conceptnet.io/c/en/%s".formatted(word);
    }
}
