package org.example;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreEntityMention;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import io.github.semlink.verbnet.*;
import io.github.semlink.verbnet.restrictions.VnRestrictions;
import io.github.semlink.verbnet.semantics.VnSemanticPredicate;
import io.github.semlink.verbnet.syntax.VnNounPhrase;
import io.github.semlink.verbnet.syntax.VnSyntax;

import java.util.*;

public class FrameMatcher {
    private static VnIndex vnIndex = DefaultVnIndex.fromDirectory("src/main/resources/verbnet3.3/xml");
    private static List<HashMap<Integer, IndexedWord>> verbsPerSentence = new ArrayList<>();

    public static HashMap<IndexedWord, HashMap<String, ArrayList<IndexedWord>>> getNounsPerVerb() {
        return nounsPerVerb;
    }

    private static HashMap<IndexedWord, HashMap<String, ArrayList<IndexedWord>>> nounsPerVerb = new HashMap<>();
    private static HashMap<Integer, String> entityType = new HashMap<>();
    private static HashMap<IndexedWord, VnClass> selectedClasses = new HashMap<>();

    public static HashMap<ArrayList<IndexedWord>, String> getRolesPerNoun() {
        return rolesPerNoun;
    }

    private static HashMap<ArrayList<IndexedWord>, String> rolesPerNoun = new HashMap<>();

    public static HashMap<IndexedWord, List<VnSemanticPredicate>> getSemanticsPerVerb() {
        return semanticsPerVerb;
    }

    private static HashMap<IndexedWord, List<VnSemanticPredicate>> semanticsPerVerb = new HashMap<>();
    public static VnFrame matchFrame(String input) {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,coref");
        props.setProperty("coref.algorithm", "neural");

        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        CoreDocument document = new CoreDocument("Dr. Rodriguez published a paper in Nature Journal from MIT about using CRISPR-Cas9 to edit genomes while Pfizer and Moderna were developing mRNA vaccines for COVID-19 during the pandemic that the WHO declared in March 2020 for 10 minutes in the 10 percent of his time. He appreciates his science in Greece for 300 euros.");
        pipeline.annotate(document);

        for (CoreSentence sentence : document.sentences()) {
            HashMap<Integer, IndexedWord> verbs = new HashMap<>();

            for (CoreLabel token : sentence.tokens()) {
                System.out.println(token.index() + " " + token.lemma() + " " + token.ner());
            }

            for (SemanticGraphEdge semanticGraphEdge : sentence.dependencyParse().edgeIterable()) {
                IndexedWord source = semanticGraphEdge.getSource();
                if (source.tag().contains("NN") && semanticGraphEdge.getTarget().tag().contains("NN")) {
                    System.out.println(semanticGraphEdge + " " + semanticGraphEdge.getRelation());
                }

                if (source.tag().contains("VB") && verbs.get(source.index()) == null) {
                    verbs.put(source.index(), source);
                    nounsPerVerb.put(source, new HashMap<>());
                }
                
                if (source.tag().contains("VB") && semanticGraphEdge.getRelation().getShortName().contains("subj")) {
                    extractNounsPerVerb(nounsPerVerb, source, "subj", semanticGraphEdge);
                } else if (source.tag().contains("VB") && semanticGraphEdge.getRelation().getShortName().contains("obj")) {
                    extractNounsPerVerb(nounsPerVerb, source, "obj", semanticGraphEdge);
                }

            }

            verbsPerSentence.add(verbs);
        }

        extractEntityMentions(document);

        for (int i = 0; i < verbsPerSentence.size(); i++) {
            HashMap<Integer, IndexedWord> sentence = verbsPerSentence.get(i);

            for (IndexedWord verb : sentence.values()) {
                extractSelectedClass(verb, i);
            }
        }

        return null;
    }

    public static HashMap<IndexedWord, VnClass> getSelectedClasses() {
        return selectedClasses;
    }

    private static void extractSelectedClass(IndexedWord verb, int i) {
        Set<VnClass> candidateClasses = new HashSet<>();
        candidateClasses.addAll(vnIndex.getByLemma(verb.lemma()));
        for (VnClass vnClass : candidateClasses) {
            String subjectRole = null, objectRole = null;

            for (VnFrame vnFrame : vnClass.framesIncludeInherited()
                    .stream().filter(vnFrame -> vnFrame.description().primary()
                            .equals("NP V NP")).toList()
            ) {
                semanticsPerVerb.put(verb, vnFrame.predicates());

                List<VnSyntax> syntax = vnFrame.syntax();
                for (VnSyntax vnSyntax : syntax) {
                    if (vnSyntax instanceof VnNounPhrase vnNounPhrase) {
                        if (subjectRole == null) {
                            subjectRole = vnNounPhrase.thematicRole();
                        } else {
                            objectRole = vnNounPhrase.thematicRole();
                        }
                    }
                }
            }

            List<VnThematicRole> classRoles = candidateClasses.stream().map(VnClass::roles).flatMap(List::stream).toList();
            boolean isSubjectOkay = classRoles.isEmpty(), isObjectOkay = classRoles.isEmpty();

            for (VnThematicRole role : classRoles) {
                HashMap<String, ArrayList<IndexedWord>> nouns = nounsPerVerb.get(verb);

                if (Objects.equals(role.type(), subjectRole)) {
                    isSubjectOkay = isNounOkay(nouns, "subj", role, isSubjectOkay);
                } else if (isSubjectOkay && Objects.equals(role.type(), objectRole)) {
                    isObjectOkay = isNounOkay(nouns, "obj", role, isObjectOkay);
                }
            }


            if (isSubjectOkay && isObjectOkay) {
                rolesPerNoun.put(nounsPerVerb.get(verb).get("subj"), subjectRole);
                rolesPerNoun.put(nounsPerVerb.get(verb).get("obj"), objectRole);

                selectedClasses.put(verb, vnClass);
                break;
            }
        }
    }

    private static boolean isNounOkay(HashMap<String, ArrayList<IndexedWord>> nouns, String pos, VnThematicRole role, boolean isNounOkay) {
        Integer subjectIndex = nouns.get(pos) != null ? nouns.get(pos).getFirst().index() : null;
        String subjectEntityType = subjectIndex != null ? entityType.get(subjectIndex) : null;
        if (!role.restrictions().isEmpty() || subjectEntityType != null) {
            for (VnRestrictions<String> vnRestrictions : role.restrictions()) {
                if (vnRestrictions.include().contains(subjectEntityType)) {
                    isNounOkay = true;
                    break;
                }
                if (!vnRestrictions.exclude().contains(subjectEntityType)) {
                    isNounOkay = true;
                    break;
                }
            }
        } else {
            isNounOkay = true;
        }
        return isNounOkay;
    }

    private static void extractNounsPerVerb(HashMap<IndexedWord, HashMap<String, ArrayList<IndexedWord>>> nounsPerVerb, IndexedWord source, String pos, SemanticGraphEdge semanticGraphEdge) {
        HashMap<String, ArrayList<IndexedWord>> grammar = nounsPerVerb.get(source) == null ? new HashMap<>() : nounsPerVerb.get(source);
        if (grammar.get(pos) == null) {
            ArrayList<IndexedWord> nouns = new ArrayList<>();
            nouns.add(semanticGraphEdge.getTarget());
            grammar.put(pos, nouns);
        } else {
            grammar.get(pos).add(semanticGraphEdge.getTarget());
        }
    }

    private static void extractEntityMentions(CoreDocument document) {
        for (CoreEntityMention em : document.entityMentions()) {
            for (CoreLabel token : em.tokens()) {
                switch (em.entityType()) {
                    case ("PERSON"):
                        entityType.put(token.index(), "animate");
                        break;
                    case ("CITY"), ("COUNTRY"):
                        entityType.put(token.index(), "location");
                        break;
                    case ("ORGANIZATION"):
                        entityType.put(token.index(), "organization");
                        break;
                    case ("MONEY"):
                        entityType.put(token.index(), "currency");
                        break;
                    default:
                        break;

                }
                System.out.println(token.word() + " " + em.entityType());
            }
        }
    }
}
