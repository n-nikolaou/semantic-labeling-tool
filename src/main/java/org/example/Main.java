package org.example;

import org.example.models.IndexedWordModel;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(MyApplication.class);
        app.setBannerMode(Banner.Mode.OFF); // Disable the Spring banner
        app.run(args);

        VerbNetAdapter adapter = new VerbNetAdapter("Dr. Rodriguez published a paper in Nature Journal from MIT about using CRISPR-Cas9 to edit genomes while Pfizer and Moderna were developing mRNA vaccines for COVID-19 during the pandemic that the WHO declared in March 2020 for 10 minutes in the 10 percent of his time. He appreciates his science in Greece for 300 euros.");

        ArrayList<IndexedWordModel> indexedWordModels = adapter.getIndexedWordModels();
//        for (IndexedWordModel indexedWordModel : indexedWordModels) {
//            System.out.println(indexedWordModel);
//        }

        GraphDBMapper mapper = new GraphDBMapper(adapter);
    }

}