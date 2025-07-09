package org.example;

import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.IntTuple;
import org.example.models.IndexedWordModel;
import org.example.models.Quotation;

import java.util.*;

public class SemanticParser {
    private static CoreDocument document;
    private static Annotation annotation;
    private static boolean isAnalysisComplete = false;

    public static CoreDocument annotateText(String input) {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,coref,depparse,natlog,openie,quote");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        CoreDocument document = new CoreDocument(input);
        pipeline.annotate(document);

        isAnalysisComplete = true;
        SemanticParser.document = document;

        Annotation doc = new Annotation(input);
        pipeline.annotate(doc);
        SemanticParser.annotation = doc;

        return document;
    }

    public static ArrayList<List<Integer>> extractTriples(ArrayList<Integer> wordsPerSentence) {
        ArrayList<List<Integer>> indices = new ArrayList<>();

        int sentenceIndex = 1;
        for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
            int finalSentenceIndex = sentenceIndex;
            indices.addAll(
                    sentence.get(NaturalLogicAnnotations.RelationTriplesAnnotation.class)
                            .stream()
                            .filter(triple -> triple.confidence > 0.5)
                            .map(triple -> triple.asSentence()
                                            .stream()
                                            .map(token ->
                                                    getGlobalIndex(wordsPerSentence, finalSentenceIndex, token.index())
                                            )
                                            .toList()
                            )
                            .toList()
            );
            sentenceIndex++;
        }

        return indices;
    }

    public static ArrayList<Quotation> extractQuotations() {
        ArrayList<Quotation> quotations = new ArrayList<>();
        for (CoreMap map : annotation.get(CoreAnnotations.QuotationsAnnotation.class)) {
            quotations.add(new Quotation(
                    map.hashCode(),
                    map.toString(),
                    map.get(QuoteAttributionAnnotator.SpeakerAnnotation.class),
                    new ArrayList<>(map.get(CoreAnnotations.TokensAnnotation.class)))
            );
        }
        return quotations;
    }

    public static HashMap<Integer, ArrayList<IntTuple>> extractMentions(ArrayList<Integer> wordsPerSentence) {
        HashMap<Integer, ArrayList<IntTuple>> mentions = new HashMap<>();
        for (CorefChain cc : annotation.get(CorefCoreAnnotations.CorefChainAnnotation.class).values()) {
            ArrayList<IntTuple> indices = new ArrayList<>();
            for (CorefChain.CorefMention mention : cc.getMentionsInTextualOrder()) {
                indices.add(new IntTuple(new int[]{
                        getGlobalIndex(wordsPerSentence, mention.position.get(0), mention.startIndex) - 1,
                        getGlobalIndex(wordsPerSentence, mention.position.get(0), mention.endIndex) - 2
                }));
            }
            mentions.put(cc.getChainID(), indices);
        }

        return mentions;
    }

    public static HashMap<Integer, ArrayList<IndexedWordModel.GrammaticalRelation>> extractGrammaticalRelations() {
        HashMap<Integer, ArrayList<IndexedWordModel.GrammaticalRelation>> relationsPerWord = new HashMap<>();

        for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
            SemanticGraph semanticGraph = sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
            for (SemanticGraphEdge edge : semanticGraph.edgeIterable()) {
                ArrayList<IndexedWordModel.GrammaticalRelation> relations = relationsPerWord.getOrDefault(
                        edge.getGovernor().beginPosition(), new ArrayList<>()
                );

                relations.stream()
                        .filter(relation -> Objects.equals(
                                relation.grammaticalRelation,
                                edge.getRelation().getShortName())
                        )
                        .findFirst()
                        .ifPresentOrElse(
                                grammaticalRelation -> grammaticalRelation.addTargetIndex(edge.getTarget().beginPosition()),
                                () -> relations.add(
                                        new IndexedWordModel.GrammaticalRelation(
                                                edge.getTarget().beginPosition(), edge.getRelation().getShortName()
                                        )
                                )
                        );

                relationsPerWord.put(edge.getGovernor().beginPosition(), relations);
            }
        }
        return relationsPerWord;
    }

    private static int getGlobalIndex(ArrayList<Integer> wordsPerSentence, int sentence, int index) {
        sentence--;
        int globalIndex = 0;
        for (int i = 0; i < sentence; i++) {
            globalIndex += wordsPerSentence.get(i);
        }
        globalIndex += index;
        return globalIndex;
    }

    public static HashMap<CoreLabel, String[]> extractEntityMentions() {
        HashMap<CoreLabel, String[]> entityType = new HashMap<>();

        for (CoreEntityMention em : SemanticParser.document.entityMentions()) {
            for (CoreLabel token : em.tokens()) {
                switch (em.entityType()) {
                    case ("PERSON"):
                        entityType.put(token, new String[]{"animate"});
                        break;
                    case ("CITY"), ("COUNTRY"):
                        entityType.put(token, new String[]{"location", "loc", "region"});
                        break;
                    case ("ORGANIZATION"):
                        entityType.put(token, new String[]{"organization"});
                        break;
                    case ("MONEY"):
                        entityType.put(token, new String[]{"currency"});
                        break;
                    default:
                        break;

                }

            }
        }

        return entityType;
    }
}
