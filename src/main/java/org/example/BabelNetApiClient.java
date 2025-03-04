package org.example;

import org.eclipse.rdf4j.model.Model;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class BabelNetApiClient extends SemanticApiClient {
    Model model;
    static String key = "3d226040-a30f-45de-ab60-1b22b508b431";

    BabelNetApiClient(ArrayList<String> words) throws ExecutionException, InterruptedException {
        super(words);
        urls = super.words.stream().map(this::getApiUrl).collect(Collectors.toList());

        List<HttpResponse<String>> responses = sendHttpRequests(urls);
        JSONArray[] jsonArrays = responses.stream().map(response -> (JSONArray) JSONValue.parse(response.body())).toArray(JSONArray[]::new);

        ListIterator<JSONArray> iterator = List.of(jsonArrays).listIterator();
        while (iterator.hasNext()) {
            int index = iterator.nextIndex();
            wordsId.putIfAbsent(words.get(index), getSynsetId(iterator.next(), words.get(index)));
        }

        for (String word : wordsId.keySet()) {
            System.out.println(word + ' ' + wordsId.get(word));
        }
    }

    public String getSynsetId(@NotNull JSONArray jsonArray, String word)  {
        for (Object o : jsonArray) {
            JSONObject jsonObject = (JSONObject) JSONValue.parse(String.valueOf(((JSONObject) o).get("properties")));
            if (jsonObject.get("fullLemma").toString().equals(word)) {
                JSONObject synsetId = (JSONObject) jsonObject.get("synsetID");
                return synsetId.get("id").toString();
            }
        }
        return null;
    }

    @Override
    public String getApiUrl(String word) {
        return "https://babelnet.io/v9/getSenses?lemma=%s&searchLang=%s&key=%s".formatted(word, "EN", key);
    }

}
