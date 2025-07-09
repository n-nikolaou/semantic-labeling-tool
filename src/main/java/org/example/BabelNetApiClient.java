package org.example;

import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

public class BabelNetApiClient extends SemanticApiClient {
    List<String> posTags;
    private final Map<String, String> synsetIdPerLemma = new HashMap<>();
    private final Map<String, List<String>> relevantWordsPerLemma = new HashMap<>();

    BabelNetApiClient(Adapter adapter) {
        super(adapter);

        posTags = words.stream().map(word -> word.posTag).collect(Collectors.toList());

        for (int i = 0; i < lemmas.size(); i++) {
            String lemma = lemmas.get(i);
            if (lemma.matches("[a-zA-Z]+")) {
                urls.set(i, getApiUrl(lemma));
            }
        }

        List<JSONArray> jsonArrays = new ArrayList<>(Collections.nCopies(urls.size(), null));
        for (int i = 0; i < urls.size(); i++) {
            String url = urls.get(i);
            if (url == null) continue;

            HttpResponse<String> response = sendHttpRequest(url);
            try {
                jsonArrays.set(i, (JSONArray) JSONValue.parse(response.body()));
            } catch (Exception ignored) {}
        }

        ListIterator<JSONArray> iterator = jsonArrays.listIterator();
        while (iterator.hasNext()) {
            int index = iterator.nextIndex();
            JSONArray jsonArray = iterator.next();
            synsetIdPerLemma.put(
                    lemmas.get(index),
                    jsonArray == null
                            ? null
                            : getSynsetId(jsonArray, lemmas.get(index), posTags.get(index))
            );
            relevantWordsPerLemma.put(
                    lemmas.get(index),
                    jsonArray == null
                            ? null
                            : getRelevantWords(jsonArray)
            );
        }
    }

    private List<String> getRelevantWords(JSONArray jsonArray) {
        List<String> relevantWords = new ArrayList<>();
        for (int i = 0; i < 10 && i < jsonArray.size(); i++) {
            Object object = jsonArray.get(i);
            JSONObject jsonObject = (JSONObject) JSONValue.parse(String.valueOf(((JSONObject) object).get("properties")));
            relevantWords.add(jsonObject.get("simpleLemma").toString().replace("_", " "));
        }

        return relevantWords;
    }

    private String getSynsetId(@NotNull JSONArray jsonArray, String lemma, String POStag)  {
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

    public Map<String, List<String>> getRelevantWordsPerLemma() {
        return relevantWordsPerLemma;
    }

    public Map<String, String> getSynsetIdPerLemma() {
        return synsetIdPerLemma;
    }

    @Override
    public String getApiUrl(String word) {
//        String key = "3d226040-a30f-45de-ab60-1b22b508b431";
        String key = "20324fab-aded-470f-9660-8a6ad0b75272";
        return "https://babelnet.io/v9/getSenses?lemma=%s&searchLang=%s&key=%s".formatted(word, "EN", key);
    }

}
