package org.example;

import org.apache.tomcat.util.json.JSONParserTokenManager;
import org.example.models.IndexedWordModel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;

import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) throws ExecutionException, InterruptedException, ParseException {
        SpringApplication app = new SpringApplication(MyApplication.class);
        app.setBannerMode(Banner.Mode.OFF); // Disable the Spring banner
        app.run(args);


//
//        BabelNetApiClient babelNetApiClient = new BabelNetApiClient(adapter);
//        ConceptNetApiClient conceptNetApiClient = new ConceptNetApiClient(adapter);
//
//        ArrayList<String> babelURLs = new ArrayList<>(), conceptURLs = new ArrayList<>();
//        for (IndexedWordModel indexedWordModel : indexedWordModels) {
//            babelURLs.add(babelNetApiClient.getApiUrl(indexedWordModel.lemma));
//            conceptURLs.add(conceptNetApiClient.getApiUrl(indexedWordModel.lemma));
//        }
//        for (HttpResponse<String> response : babelNetApiClient.sendHttpRequests(babelURLs)) {
//            JSONArray array = (JSONArray) new JSONParser().parse(response.body());
//            for (Object object : array) {
//                JSONObject obj = (JSONObject) object;
//                System.out.println(obj.toJSONString());
//            }
//        }
//        for (HttpResponse<String> response : conceptNetApiClient.sendHttpRequests(conceptURLs)) {
//            JSONObject array = (JSONObject) new JSONParser().parse(response.body());
//            System.out.println(array.toJSONString());
//        }
    }

}