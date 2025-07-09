package org.example;

import org.codehaus.commons.nullanalysis.NotNull;
import org.example.models.IndexedWordModel;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public abstract class SemanticApiClient {
    List<String> lemmas, urls;
    List<IndexedWordModel> words;

    SemanticApiClient(Adapter adapter) {
        this.words = adapter.getIndexedWordModels();
        words.sort((a, b) -> a.index > b.index ? 1 : -1);
        this.lemmas = words.stream().map(word -> word.lemma).collect(Collectors.toList());
        urls = new ArrayList<>(Collections.nCopies(lemmas.size(), null));
    }

    protected List<HttpResponse<String>> sendHttpRequests(List<String> urls) throws CancellationException {
        HttpClient client = HttpClient.newHttpClient();

        return urls.stream()
                .filter(Objects::nonNull)
                .map((url) -> HttpRequest.newBuilder().uri(URI.create(url)).GET().build())
                .map(request -> client.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
                .map(future -> {
                    try {
                        return future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }).toList();
    }

    protected HttpResponse<String> sendHttpRequest(@NotNull String url) throws CancellationException {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        CompletableFuture<HttpResponse<String>> future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }
    }

    protected abstract String getApiUrl(String word);
}
