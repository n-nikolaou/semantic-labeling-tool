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

    ConceptNetApiClient(ArrayList<String> words) throws ExecutionException, InterruptedException {
        super(words);
        urls = super.words.stream().map(this::getApiUrl).collect(Collectors.toList());

        List<HttpResponse<String>> responses = sendHttpRequests(urls);

        JSONObject[] jsonObjects = responses.stream().map(response -> JSONValue.parse(response.body())).toArray(JSONObject[]::new);

        ListIterator<JSONObject> iterator = List.of(jsonObjects).listIterator();
        while (iterator.hasNext()) {
            int index = iterator.nextIndex();
            wordsId.putIfAbsent(words.get(index), getUriId(iterator.next()));
        }

        for (String word : wordsId.keySet()) {
            System.out.println(word + ' ' + wordsId.get(word));
        }
    }

    private String getUriId(JSONObject jsonObject) {
        return jsonObject.get("@id").toString();
    }

    @Override
    protected String getApiUrl(String word) {
        return "https://api.conceptnet.io/c/en/%s".formatted(word);
    }
}
