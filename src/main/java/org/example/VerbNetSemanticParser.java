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

public class VerbNetSemanticParser {
    private static VnIndex vnIndex = DefaultVnIndex.fromDirectory("src/main/resources/verbnet3.3/xml");
    private static CoreDocument document;
    private static boolean isAnalysisComplete = false;

    public static CoreDocument annotateText(String input) {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,coref");
        props.setProperty("coref.algorithm", "neural");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

//        CoreDocument document = new CoreDocument("Dr. Rodriguez published a paper in Nature Journal from MIT about using CRISPR-Cas9 to edit genomes while Pfizer and Moderna were developing mRNA vaccines for COVID-19 during the pandemic that the WHO declared in March 2020 for 10 minutes in the 10 percent of his time. He appreciates his science in Greece for 300 euros.");
        CoreDocument document = new CoreDocument(input);
        pipeline.annotate(document);

        isAnalysisComplete = true;
        VerbNetSemanticParser.document = document;
        return document;
    }

    public static HashSet<IndexedWord> extractVerbs() {
        HashSet<IndexedWord> verbs = new HashSet<>();

        for (CoreSentence sentence : VerbNetSemanticParser.document.sentences()) {
            for (SemanticGraphEdge edge : sentence.dependencyParse().edgeIterable()) {
                IndexedWord source = edge.getSource();

                if (source.tag().contains("VB")) {
                    verbs.add(source);
                }
            }
        }

        return verbs;
    }

    public static HashMap<IndexedWord, HashMap<String, ArrayList<IndexedWord>>> extractNounsRelations(HashSet<IndexedWord> verbs) {
        HashMap<IndexedWord, HashMap<String, ArrayList<IndexedWord>>> nounsPerVerb = new HashMap<>();

        for (CoreSentence sentence : VerbNetSemanticParser.document.sentences()) {
            for (SemanticGraphEdge semanticGraphEdge : sentence.dependencyParse().edgeIterable()) {
                IndexedWord source = semanticGraphEdge.getSource();

                if (verbs.contains(source) && semanticGraphEdge.getRelation().getShortName().contains("subj")) {
                    addNounsToVerb(nounsPerVerb, source, "subj", semanticGraphEdge);
                } else if (source.tag().contains("VB") && semanticGraphEdge.getRelation().getShortName().contains("obj")) {
                    addNounsToVerb(nounsPerVerb, source, "obj", semanticGraphEdge);
                }

            }
        }

        return nounsPerVerb;
    }

    private static void addNounsToVerb(HashMap<IndexedWord, HashMap<String, ArrayList<IndexedWord>>> nounsPerVerb, IndexedWord source, String pos, SemanticGraphEdge semanticGraphEdge) {
        HashMap<String, ArrayList<IndexedWord>> subjObjList = nounsPerVerb.get(source) == null
                ? new HashMap<>()
                : nounsPerVerb.get(source);
        if (subjObjList.get(pos) == null) {
            ArrayList<IndexedWord> nouns = new ArrayList<>();
            nouns.add(semanticGraphEdge.getTarget());
            subjObjList.put(pos, nouns);
        } else {
            subjObjList.get(pos).add(semanticGraphEdge.getTarget());
        }
        nounsPerVerb.put(source, subjObjList);
    }

    public static Map<Integer, String> extractNounRelations(IndexedWord word) {
        for (CoreSentence sentence : VerbNetSemanticParser.document.sentences()) {
            for (SemanticGraphEdge semanticGraphEdge : sentence.dependencyParse().edgeIterable()) {
                IndexedWord source = semanticGraphEdge.getSource();

                if (source.beginPosition() == word.beginPosition()) {
                    if (source.tag().contains("NN")) {
                        return Map.of(semanticGraphEdge.getTarget().index(), semanticGraphEdge.getRelation().getLongName());
                    }
                }
            }
        }

        return null;
    }

    public static FrameExtractionResult extractFrame(
            IndexedWord verb,
            HashMap<String, ArrayList<IndexedWord>> nouns,
            HashMap<CoreLabel, String[]> entityType
    ) {
        Set<VnClass> candidateClasses = new HashSet<>(vnIndex.getByLemma(verb.lemma()));

        IdentityHashMap<VnFrame, Integer> rankedClasses = new IdentityHashMap<>();
        String bestSubjectRole = null, bestObjectRole = null;
        VnFrame bestFrame = null;

        for (VnClass vnClass : candidateClasses) {
            VnFrame frame = vnClass
                    .framesIncludeInherited().stream()
                    .filter(vnFrame ->
                            vnFrame.description().primary()
                                    .equals("NP V NP")
                    )
                    .findFirst()
                    .orElse(null);


            String subjectRole = null, objectRole = null;

            if (frame != null) {
                rankedClasses.put(frame, 0);

                if (vnClass.roles().isEmpty()) {
                    rankedClasses.put(frame, 8);
                    break;
                }

                for (VnSyntax vnSyntax : frame.syntax()) {
                    if (vnSyntax instanceof VnNounPhrase vnNounPhrase) {
                        if (subjectRole == null) {
                            subjectRole = vnNounPhrase.thematicRole();
                            checkRestrictions(subjectRole, nouns, entityType, frame, vnClass, rankedClasses, "subj");
                        } else {
                            objectRole = vnNounPhrase.thematicRole();
                            checkRestrictions(objectRole, nouns, entityType, frame, vnClass, rankedClasses, "obj");
                        }
                    }
                }

                if (rankedClasses.get(frame) > rankedClasses.getOrDefault(bestFrame, -1)) {
                    bestFrame = frame;
                    bestSubjectRole = subjectRole;
                    bestObjectRole = objectRole;
                }
            }
        }

        return new FrameExtractionResult(bestFrame.predicates(), bestSubjectRole, bestObjectRole );

    }

    private static void checkRestrictions(
            String role,
            HashMap<String, ArrayList<IndexedWord>> nouns,
            HashMap<CoreLabel, String[]> entityType,
            VnFrame frame,
            VnClass vnClass,
            IdentityHashMap<VnFrame, Integer> rankedClasses,
            String pos
    ) {
        if (role == null) {
            rankedClasses.put(frame, rankedClasses.get(frame) + 1);
            return;
        }

        if (nouns.get(pos) == null) {
            return;
        }
        rankedClasses.put(frame, rankedClasses.get(frame) + 1);


        CoreLabel noun = nouns.get(pos).get(0).backingLabel();

        if (entityType.get(noun) == null) {
            return;
        }
        rankedClasses.put(frame, rankedClasses.get(frame) + 1);

        String ner = entityType.get(noun)[0];
        VnThematicRole foundRole = vnClass.roles()
                .stream()
                .filter(thematicRole -> Objects.equals(thematicRole.type(), role))
                .findFirst()
                .orElse(null);

        if (foundRole != null) {
            for (VnRestrictions<String> roleRestriction : foundRole.restrictions()) {
                if (roleRestriction.exclude().contains(ner)) {
                    return;
                }
                if (roleRestriction.include().contains(ner)) {
                    rankedClasses.put(frame, rankedClasses.get(frame) + 1);
                    return;
                }
            }
        }

    }

    public static HashMap<CoreLabel, String[]> extractEntityMentions() {
        HashMap<CoreLabel, String[]> entityType = new HashMap<>();

        for (CoreEntityMention em : VerbNetSemanticParser.document.entityMentions()) {
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

    public static boolean isAnalysisComplete() {
        return isAnalysisComplete;
    }

    public static class FrameExtractionResult {
        private List<VnSemanticPredicate> predicates;
        private String subjectRole;
        private String objectRole;

        public FrameExtractionResult(List<VnSemanticPredicate> predicates, String subjectRole, String objectRole) {
            this.predicates = predicates;
            this.subjectRole = subjectRole;
            this.objectRole = objectRole;
        }

        // Getters
        public List<VnSemanticPredicate> getPredicates() { return predicates; }
        public String getSubjectRole() { return subjectRole; }
        public String getObjectRole() { return objectRole; }
    }
}
