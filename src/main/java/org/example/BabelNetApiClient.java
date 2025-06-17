package org.example;

import org.eclipse.rdf4j.model.Model;
import org.example.models.IndexedWordModel;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class BabelNetApiClient extends SemanticApiClient {
    List<String> posTags;
    final String key = "3d226040-a30f-45de-ab60-1b22b508b431";

    BabelNetApiClient(VerbNetAdapter verbNetAdapter) throws ExecutionException, InterruptedException {
        super(verbNetAdapter);
        urls = lemmas.stream().map(this::getApiUrl).collect(Collectors.toList());
        posTags = words.stream().map(word -> word.posTag).collect(Collectors.toList());

        List<HttpResponse<String>> responses = sendHttpRequests(urls);
//        JSONArray[] jsonArrays = responses.stream().map(response -> (JSONArray) JSONValue.parse(response.body())).toArray(JSONArray[]::new);
//
//        ListIterator<JSONArray> iterator = List.of(jsonArrays).listIterator();
//        while (iterator.hasNext()) {
//            int index = iterator.nextIndex();
//            wordsId.add(getSynsetId(iterator.next(), lemmas.get(index), posTags.get(index)));
//        }
//        for (int i = 0; i < lemmas.size(); i++) {
//            System.out.println(wordsId.get(i) + " " + lemmas.get(i));
//        }
    }

    public String getSynsetId(@NotNull JSONArray jsonArray, String lemma, String POStag)  {
        JSONObject candidateSynsetId = null;
        if (POStag != null) {
            for (Object o : jsonArray) {
                JSONObject jsonObject = (JSONObject) JSONValue.parse(String.valueOf(((JSONObject) o).get("properties")));
                if (jsonObject.get("simpleLemma").toString().equals(lemma)) {
                    if (jsonObject.get("pos").toString().equals(POStag)) {
                        JSONObject synsetId = (JSONObject) jsonObject.get("synsetID");
                        return synsetId.get("id").toString();
                    }
                    candidateSynsetId = (JSONObject) jsonObject.get("synsetID");
                }
            }
        }
        return candidateSynsetId == null ? null : candidateSynsetId.get("id").toString();
    }

    @Override
    public String getApiUrl(String word) {
        return "https://babelnet.io/v9/getSenses?lemma=%s&searchLang=%s&key=%s".formatted(word, "EN", key);
    }

}
