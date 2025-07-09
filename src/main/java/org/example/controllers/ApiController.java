package org.example.controllers;

import org.example.models.Edge;
import org.example.models.IndexedWordModel;
import org.example.models.Quotation;
import org.example.services.ApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ExecutionException;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api")  // Base path
public class ApiController {
    private final ApiService apiService;

    public ApiController(ApiService apiService) {
        this.apiService = apiService;
        CorsConfig corsConfig = new CorsConfig();
    }

    @GetMapping("/indexed-words")
    public ArrayList<IndexedWordModel> getIndexedWords() {
        return apiService.getIndexedWords();
    }

    @GetMapping("/grammatical-relations/{targetIndex}")
    public ArrayList<IndexedWordModel.GrammaticalRelation> getGrammaticalRelations(
            @PathVariable int targetIndex
    ) {
        return apiService.getGrammaticalRelations(targetIndex);
    }

    @GetMapping("/triple-relations/{targetIndex}")
    public ArrayList<ArrayList<Integer>> getTripleRelations(
            @PathVariable int targetIndex
    ) {
        return apiService.getRelationTriples(targetIndex);
    }

    @GetMapping("/get-conceptnet-edges/{targetIndex}")
    public List<Edge> getConceptNetEdges(@PathVariable int targetIndex) {
        return apiService.getConceptNetEdges(targetIndex);
    }

    @GetMapping("/get-babelnet-relations/{targetIndex}")
    public List<String> getBabelNetRelations(@PathVariable int targetIndex) {
        return apiService.getBabelNetRelations(targetIndex);
    }

    @GetMapping("/get-quotations")
    public List<Quotation> getQuotations() {
        return apiService.getQuotations();
    }

    @PostMapping("/annotate")
    public ResponseEntity<Map<String, String>> startProcessing(@RequestBody String text) {
        String jobId = UUID.randomUUID().toString();
        apiService.processAsync(text, jobId);

        Map<String, String> response = new HashMap<>();
        response.put("jobId", jobId);
        response.put("status", "processing");
        response.put("checkStatus", "/api/status/" + jobId);

        System.out.println(jobId);
        return ResponseEntity.accepted()
                .body(response);
    }


    @GetMapping("/status/{jobId}")
    public ResponseEntity<?> checkStatus(@PathVariable String jobId) {
        String result = apiService.getResult(jobId);
        if (result == null) {
            return ResponseEntity.ok(Map.of("status", "processing"));
        } else {
            return ResponseEntity.ok(Map.of("status", "completed", "result", result));
        }
    }

}