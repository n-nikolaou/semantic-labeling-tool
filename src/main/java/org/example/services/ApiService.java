package org.example.services;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import org.example.models.IndexedWordModel;
import org.springframework.stereotype.Service;

import java.util.*;

import static org.example.VerbNetSemanticParser.*;

@Service
public class ApiService {

    public IndexedWordModel[] getIndexedWords() {
        if (isAnalysisComplete()) {
            CoreDocument document = annotateText("");
            HashSet<IndexedWord> verbs = extractVerbs();
            HashMap<IndexedWord, HashMap<String, ArrayList<IndexedWord>>> nounsPerVerb = extractNounsRelations(verbs);

            int wordsIndex = 0;

            for (int i = 0; i < document.sentences().size(); i++) {
                CoreSentence sentence = document.sentences().get(i);

                for (int j = 0; j < sentence.tokens().size(); j++) {
                    CoreLabel token = sentence.tokens().get(j);
                    IndexedWordModel word = new IndexedWordModel(token);
                    word.index = wordsIndex;
                    word.lemma = token.lemma();
                    word.posTag = token.tag();

//                    if (getEntityType().containsKey(token)) {
//                        word.ner = getEntityType().get(token);
//                    }

                    Optional<IndexedWord> foundVerb = verbs.stream().filter(verbToFind -> verbToFind.backingLabel() == token).findFirst();

                    if (foundVerb.isPresent()) {
                        IndexedWord verb = foundVerb.get();

//                        Map<Integer, String> extractedRelations = extractRelations(verb, document);
//                        ArrayList<IndexedWordModel.GrammaticalRelation> relations = new ArrayList<>();
//                        if (extractedRelations != null) {
//                            for (Map.Entry<Integer, String> entry : extractedRelations.entrySet()) {
//                                relations.add(new IndexedWordModel.GrammaticalRelation(entry.getKey(), entry.getValue()));
//                            }
//                        }
//
//                        word.relations = relations.toArray(new IndexedWordModel.GrammaticalRelation[relations.size()]);


                    }


                    wordsIndex++;
                }
            }

//            HashMap<edu.stanford.nlp.ling.IndexedWord, List<VnSemanticPredicate>> semanticsPerVerb = FrameMatcher.getSemanticsPerVerb();
//            for (edu.stanford.nlp.ling.IndexedWord verb : semanticsPerVerb.keySet()) {
//                System.out.println(verb.word() + " " + getSelectedClasses().get(verb).verbNetId());
//                ArrayList<edu.stanford.nlp.ling.IndexedWord> objects = getNounsPerVerb().get(verb).get("obj");
//                ArrayList<edu.stanford.nlp.ling.IndexedWord> subjects = getNounsPerVerb().get(verb).get("subj");
//                String roleObject = getRolesPerNoun().get(objects);
//                String roleSubject = getRolesPerNoun().get(subjects);
//
//                System.out.println(roleObject + " " + objects);
//                System.out.println(roleSubject + " " + subjects);
//
//
//                for (VnSemanticPredicate verbPredicate : semanticsPerVerb.get(verb)) {
//                    System.out.println("  " + verbPredicate.type());
//                    for (VnSemanticArgument semanticArgument : verbPredicate.semanticArguments()) {
//                        ArrayList<edu.stanford.nlp.ling.IndexedWord> nouns = semanticArgument.value().equals(roleObject)
//                                ? objects
//                                : semanticArgument.value().equals(roleSubject) ? subjects : null;
//                        if (nouns != null || !Objects.equals(semanticArgument.type(), "ThemRole")) {
//                            System.out.println("    " + semanticArgument.type() + " " + ((nouns != null) ? nouns : semanticArgument.value()));
//                        }
//                    }
//                }
//            }
        }

        return null;
    }
}
