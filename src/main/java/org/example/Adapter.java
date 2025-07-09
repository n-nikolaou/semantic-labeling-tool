package org.example;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.util.IntTuple;
import org.example.models.Edge;
import org.example.models.IndexedWordModel;
import org.example.models.Quotation;

import java.util.*;

import static org.example.SemanticParser.*;

public class Adapter {
    String input;
    List<IndexedWordModel> indexedWordModels = new ArrayList<>();
    List<List<Integer>> relationTriples = new ArrayList<>();
    Map<Integer, ArrayList<IntTuple>> mentions = new HashMap<>();
    Map<Integer, ArrayList<IndexedWordModel.GrammaticalRelation>> grammaticalRelations = new HashMap<>();
    Map<String, List<Edge>> edges = new HashMap<>();
    Map<String, List<String>> relevantWordsPerLemma = new HashMap<>();
    Map<String, String> synsetIdPerLemma = new HashMap<>();
    ArrayList<Quotation> quotations = new ArrayList<>();

    public Adapter(String input) {
        this.input = input;
    }

    private IntTuple getMentionId(int index) {
        for (Map.Entry<Integer, ArrayList<IntTuple>> entry : mentions.entrySet()) {
            for (IntTuple tuple : entry.getValue()) {
                if (tuple.get(0) <= index && tuple.get(1) >= index) {
                    return new IntTuple(new int[]{entry.getKey(), tuple.hashCode()});
                }
            }
        }
        return null;
    }

    public void mapWordsToModels() {
        CoreDocument document = annotateText(input);

        grammaticalRelations = extractGrammaticalRelations();
        quotations = extractQuotations();

        int wordsIndex = 0, sentenceIndex = 0;
        ArrayList<Integer> wordsPerSentence = new ArrayList<>(Collections.nCopies(document.sentences().size(), 0));

        for (CoreSentence sentence : document.sentences()) {
            for (CoreLabel token : sentence.tokens()) {
                wordsPerSentence.set(sentenceIndex, token.index());
                token.setIndex(wordsIndex);

                IndexedWordModel word = new IndexedWordModel(token);
                word.index = wordsIndex;
                wordsIndex++;

                indexedWordModels.add(word);
            }
            sentenceIndex++;
        }

        mentions = extractMentions(wordsPerSentence);
        relationTriples = extractTriples(wordsPerSentence);

        for (IndexedWordModel word : indexedWordModels) {
            CoreLabel token = word.token;
            word.word = token.word();
            word.lemma = token.lemma();
            word.posTag = token.tag();

            if (grammaticalRelations.get(word.token.beginPosition()) != null) {
                for (IndexedWordModel.GrammaticalRelation relation : grammaticalRelations.get(word.token.beginPosition())) {
                    for (int i = 0; i < relation.targetIndices.size(); i++) {
                        int targetIndex = relation.targetIndices.get(i);

                        indexedWordModels.stream()
                                .filter(indexedWord -> indexedWord.token.beginPosition() == targetIndex)
                                .findFirst()
                                .ifPresent(indexedWord -> {

                                    if (word.relations == null) {
                                        word.relations = new ArrayList<>();
                                    }
                                    word.relations.add(
                                            new IndexedWordModel.GrammaticalRelation(
                                                    indexedWord.index, relation.grammaticalRelation
                                            ));
                                });
                    }
                }
            }

            if (getMentionId(word.index) != null) {
                word.chainMentionId = getMentionId(word.index).get(0);
                word.mentionId = getMentionId(word.index).get(1);
            }

            for (Quotation quotation : quotations) {
                if (quotation.isTokenInsideQuotation(token)) {
                    word.quotation = quotation;
                    quotation.addIndexedWord(word);
                    break;
                }
            }

            HashMap<CoreLabel, String[]> entityMentions = extractEntityMentions();
            if (entityMentions.containsKey(token)) {
                word.ner = entityMentions.get(token)[0];
            }
        }

        exportExternalInformation();
    }

    public void exportExternalInformation() {
        BabelNetApiClient bnClient = new BabelNetApiClient(this);
        relevantWordsPerLemma = bnClient.getRelevantWordsPerLemma();
        synsetIdPerLemma = bnClient.getSynsetIdPerLemma();
        ConceptNetApiClient cnClient = new ConceptNetApiClient(this);
        edges = cnClient.getEdges();
    }

    public Map<String, List<Edge>> getEdges() {
        return edges;
    }

    public List<IndexedWordModel> getIndexedWordModels() {
        return indexedWordModels;
    }

    public List<List<Integer>> getRelationTriples() {
        return relationTriples;
    }

    public List<Quotation> getQuotations() {
        return quotations;
    }

    public Map<String, String> getSynsetIdPerLemma() {
        return synsetIdPerLemma;
    }

    public Map<String, List<String>> getRelevantWordsPerLemma() {
        return relevantWordsPerLemma;
    }
}
