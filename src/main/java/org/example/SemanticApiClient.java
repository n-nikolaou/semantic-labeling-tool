package org.example;

import org.example.models.IndexedWordModel;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public abstract class SemanticApiClient {
    List<String> lemmas, wordsId = new ArrayList<>();
    ArrayList<IndexedWordModel> words;
    List<String> urls;

    SemanticApiClient(VerbNetAdapter verbNetAdapter) {
        this.words = verbNetAdapter.getIndexedWordModels();
        words.sort((a, b) -> a.index > b.index ? 1 : -1);
        this.lemmas = words.stream().map(word -> word.lemma).collect(Collectors.toList());
    }

    protected List<HttpResponse<String>> sendHttpRequests(List<String> urls) throws CancellationException, ExecutionException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();

        return urls.stream()
                .map(url -> HttpRequest.newBuilder().uri(URI.create(url)).GET().build())
                .map(request -> client.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
                .map(future -> {
                    try {
                        return future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }).toList();

    }

    protected abstract String getApiUrl(String word);
}
