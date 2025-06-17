package org.example;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import io.github.semlink.verbnet.semantics.VnSemanticArgument;
import io.github.semlink.verbnet.semantics.VnSemanticPredicate;
import org.example.models.IndexedWordModel;
import org.example.models.SemanticArgument;
import org.example.models.VerbDetails;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.example.VerbNetSemanticParser.*;

public class VerbNetAdapter {
    String input;
    ArrayList<IndexedWordModel> indexedWordModels = new ArrayList<>();

    public VerbNetAdapter(String input) {
        this.input = input;
    }

    public void mapWordsToModels() {
        CoreDocument document = annotateText(input);

        HashSet<IndexedWord> verbs = extractVerbs();
        HashMap<IndexedWord, HashMap<String, ArrayList<IndexedWord>>> nounsPerVerb = extractNounsRelations(verbs);

        int wordsIndex = 0;

        for (CoreSentence sentence : document.sentences()) {
            for (CoreLabel token : sentence.tokens()) {
                token.setIndex(wordsIndex);

                IndexedWordModel word = new IndexedWordModel(token);
                word.index = wordsIndex;
                wordsIndex++;

                indexedWordModels.add(word);
            }
        }


        for (IndexedWordModel word : indexedWordModels) {
            CoreLabel token = word.token;
            word.word = token.word();
            word.lemma = token.lemma();
            word.posTag = token.tag();

            HashMap<CoreLabel, String[]> entityMentions = extractEntityMentions();
            if (entityMentions.containsKey(token)) {
                word.ner = entityMentions.get(token)[0];
                System.out.println(word.ner);
            }

            Optional<IndexedWord> foundVerb = verbs.stream().filter(
                    verbToFind -> verbToFind.backingLabel() == token
            ).findFirst();

            if (foundVerb.isPresent()) {
                IndexedWord verb = foundVerb.get();
                if (nounsPerVerb.get(verb) != null) {
                    FrameExtractionResult result = extractFrame(verb, nounsPerVerb.get(verb), entityMentions);
                    List<VnSemanticPredicate> semanticPredicates = result.getPredicates();
                    if (semanticPredicates != null) {
                        SemanticArgument[] arguments = new SemanticArgument[semanticPredicates.size()];

                        word.verbDetails = new VerbDetails();

                        word.verbDetails.thematicRoles = new VerbDetails.ThematicRole[2];
                        word.verbDetails.thematicRoles[0] = new VerbDetails.ThematicRole();
                        word.verbDetails.thematicRoles[1] = new VerbDetails.ThematicRole();
                        word.verbDetails.thematicRoles[0].wordIndex = nounsPerVerb.get(verb).get("subj") == null
                                ? null
                                : nounsPerVerb.get(verb).get("subj").get(0).index();
                        word.verbDetails.thematicRoles[1].wordIndex = nounsPerVerb.get(verb).get("obj") == null
                                ? null
                                : nounsPerVerb.get(verb).get("obj").get(0).index();
                        word.verbDetails.thematicRoles[0].type = word.verbDetails.thematicRoles[0].wordIndex == null
                                ? null
                                : result.getSubjectRole();
                        word.verbDetails.thematicRoles[1].type = word.verbDetails.thematicRoles[1].wordIndex == null
                                ? null
                                : result.getObjectRole();

                        word.verbDetails.semanticArguments = arguments;

                        for (int k = 0; k < arguments.length; k++) {
                            word.verbDetails.semanticArguments[k] = new SemanticArgument();

                            VnSemanticPredicate semanticPredicate = semanticPredicates.get(k);
                            word.verbDetails.semanticArguments[k].predicate = semanticPredicate.type();
                            word.verbDetails.semanticArguments[k].arguments = new SemanticArgument.Argument[semanticPredicate.semanticArguments().size()];
                            for (int l = 0; l < semanticPredicate.semanticArguments().size(); l++) {
                                word.verbDetails.semanticArguments[k].arguments[l] = new SemanticArgument.Argument();

                                VnSemanticArgument semanticArgument = semanticPredicate.semanticArguments().get(l);
                                word.verbDetails.semanticArguments[k].arguments[l].type = semanticArgument.type();
                                word.verbDetails.semanticArguments[k].arguments[l].value = semanticArgument.value();
                            }
                        }
                    }
                }

                HashMap<String, ArrayList<IndexedWord>> subjObjLists = nounsPerVerb.get(verb);
                ArrayList<IndexedWordModel.GrammaticalRelation> relations = new ArrayList<>();

                if (subjObjLists != null) {
                    for (Map.Entry<String, ArrayList<IndexedWord>> entry : subjObjLists.entrySet()) {
                        relations.add(new IndexedWordModel.GrammaticalRelation(
                                entry.getValue().stream()
                                        .map(IndexedWord::index)
                                        .toArray(Integer[]::new),
                                entry.getKey()));
                    }
                }

                word.relations = relations.toArray(new IndexedWordModel.GrammaticalRelation[relations.size()]);

            } else {
                Map<Integer, String> extractedRelations = extractNounRelations(translateCoreLabelToIndexedWord(token, document));
                ArrayList<IndexedWordModel.GrammaticalRelation> relations = new ArrayList<>();

                if (extractedRelations != null) {
                    for (Map.Entry<Integer, String> entry : extractedRelations.entrySet()) {
                        relations.add(new IndexedWordModel.GrammaticalRelation(entry.getKey(), entry.getValue()));
                    }
                }

                word.relations = relations.toArray(new IndexedWordModel.GrammaticalRelation[relations.size()]);
            }
        }
    }

    private IndexedWord translateCoreLabelToIndexedWord(CoreLabel token, CoreDocument document) {
        for (CoreSentence sentence : document.sentences()) {
            for (SemanticGraphEdge edge : sentence.dependencyParse().edgeIterable()) {
                if (edge.getSource().beginPosition() == token.beginPosition()) {
                    return edge.getSource();
                } else if (edge.getTarget().beginPosition() == token.beginPosition()) {
                    return edge.getTarget();
                }
            }
        }
        return null;
    }

    public ArrayList<IndexedWordModel> getIndexedWordModels() {
        return indexedWordModels;
    }
}
