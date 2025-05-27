package org.example;

import edu.stanford.nlp.ling.IndexedWord;
import io.github.semlink.verbnet.semantics.VnSemanticArgument;
import io.github.semlink.verbnet.semantics.VnSemanticPredicate;
import it.unimi.dsi.fastutil.Hash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static org.example.FrameMatcher.*;

public class Main {
    public static void main(String[] args)  {
        FrameMatcher.matchFrame("");

        HashMap<IndexedWord, List<VnSemanticPredicate>> semanticsPerVerb = FrameMatcher.getSemanticsPerVerb();
        for (IndexedWord verb : semanticsPerVerb.keySet()) {
            System.out.println(verb.word() + " " + getSelectedClasses().get(verb).verbNetId());
            ArrayList<IndexedWord> objects = getNounsPerVerb().get(verb).get("obj");
            ArrayList<IndexedWord> subjects = getNounsPerVerb().get(verb).get("subj");
            String roleObject = getRolesPerNoun().get(objects);
            String roleSubject = getRolesPerNoun().get(subjects);

            System.out.println(roleObject + " " + objects);
            System.out.println(roleSubject + " " + subjects);


            for (VnSemanticPredicate verbPredicate : semanticsPerVerb.get(verb)) {
                System.out.println("  " + verbPredicate.type());
                for (VnSemanticArgument semanticArgument : verbPredicate.semanticArguments()) {
                    ArrayList<IndexedWord> nouns = semanticArgument.value().equals(roleObject)
                            ? objects
                            : semanticArgument.value().equals(roleSubject) ? subjects : null;
                    if (nouns != null || !Objects.equals(semanticArgument.type(), "ThemRole")) {
                        System.out.println("    " + semanticArgument.type() + " " + ((nouns != null) ? nouns : semanticArgument.value()));
                    }
                }
            }
        }

//        BabelNetApiClient babelNetApiClient = new BabelNetApiClient(textProcessor);
//        ConceptNetApiClient conceptNetApiClient = new ConceptNetApiClient(textProcessor);
    }
}