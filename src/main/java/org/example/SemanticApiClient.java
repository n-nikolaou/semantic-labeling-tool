package org.example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public abstract class SemanticApiClient {
    ArrayList<String> words;
    HashMap<String, String> wordsId;
    List<String> urls;

    SemanticApiClient(ArrayList<String> words) {
        this.words = words;
        wordsId = new HashMap<>();
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
