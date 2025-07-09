package org.example;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;

public class OntologyProperties {
    private static final String BASE = "http://example.org/";

    public static Map<String, IRI> createPropertyMap(ValueFactory vf) {
        Map<String, IRI> propertyMap = new HashMap<>();

        propertyMap.put("hasLemma", vf.createIRI(BASE, "hasLemma"));
        propertyMap.put("hasPartOfSpeech", vf.createIRI(BASE, "hasPartOfSpeech"));
        propertyMap.put("hasNamedEntityRecognition", vf.createIRI(BASE, "hasNamedEntityRecognition"));

        // Synset Properties
        propertyMap.put("hasRelevantWord", vf.createIRI(BASE, "hasRelevantWord"));
        propertyMap.put("hasSynsetId", vf.createIRI(BASE, "hasSynsetId"));

        // ConceptNet Properties
        propertyMap.put("forLemma", vf.createIRI(BASE, "forLemma"));
        propertyMap.put("hasStart", vf.createIRI(BASE, "hasStart"));
        propertyMap.put("hasEnd", vf.createIRI(BASE, "hasEnd"));
        propertyMap.put("hasRelationWeight", vf.createIRI(BASE, "hasRelationWeight"));
        propertyMap.put("hasLabel", vf.createIRI(BASE, "hasLabel"));
        propertyMap.put("relatedToCN", vf.createIRI(BASE, "relatedToCN"));

        for (Map.Entry<String, String> entry: ConceptNetRDFProperties.RELATION_TO_PROPERTY.entrySet()) {
            propertyMap.put(entry.getKey(), vf.createIRI(BASE, entry.getValue()));
        }

        propertyMap.put("hasChainMention", vf.createIRI(BASE, "hasChainMention"));
        propertyMap.put("hasMention", vf.createIRI(BASE, "hasMention"));
        propertyMap.put("hasId", vf.createIRI(BASE, "hasId"));
        propertyMap.put("hasWord", vf.createIRI(BASE, "hasWord"));


        propertyMap.put("hasRole", vf.createIRI(BASE, "hasRole"));
        propertyMap.put("hasThematicRole", vf.createIRI(BASE, "hasThematicRole"));
        propertyMap.put("thematicType", vf.createIRI(BASE, "thematicType"));
//        propertyMap.put("token", vf.createIRI(BASE, "token"));
        propertyMap.put("hasPredicate", vf.createIRI(BASE, "hasPredicate"));
        propertyMap.put("value", vf.createIRI(BASE, "value"));
        propertyMap.put("argument", vf.createIRI(BASE, "argument"));
        propertyMap.put("argumentType", vf.createIRI(BASE, "argumentType"));
        propertyMap.put("argumentValue", vf.createIRI(BASE, "argumentValue"));
        propertyMap.put("hasGrammaticalRelation", vf.createIRI(BASE, "hasGrammaticalRelation"));
        propertyMap.put("source", vf.createIRI(BASE, "source"));
        propertyMap.put("target", vf.createIRI(BASE, "target"));
        propertyMap.put("relation", vf.createIRI(BASE, "relation"));
        propertyMap.put("word", vf.createIRI(BASE, "word"));
        propertyMap.put("hasIndex", vf.createIRI(BASE, "hasIndex"));
        propertyMap.put("isInRelationTriple", vf.createIRI(BASE, "isInRelationTriple"));
        propertyMap.put("hasToken", vf.createIRI(BASE, "hasToken"));
        propertyMap.put("hasSpeaker", vf.createIRI(BASE, "hasSpeaker"));
        propertyMap.put("hasText", vf.createIRI(BASE, "hasText"));

        return propertyMap;
    }

    public static class ConceptNetRDFProperties {
        public static final Map<String, String> RELATION_TO_PROPERTY = new HashMap<>();

        static {
            RELATION_TO_PROPERTY.put("/r/RelatedTo", "relatedTo");
            RELATION_TO_PROPERTY.put("/r/FormOf", "formOf");
            RELATION_TO_PROPERTY.put("/r/IsA", "isA");
            RELATION_TO_PROPERTY.put("/r/PartOf", "partOf");
            RELATION_TO_PROPERTY.put("/r/HasA", "hasA");
            RELATION_TO_PROPERTY.put("/r/UsedFor", "usedFor");
            RELATION_TO_PROPERTY.put("/r/CapableOf", "capableOf");
            RELATION_TO_PROPERTY.put("/r/AtLocation", "atLocation");
            RELATION_TO_PROPERTY.put("/r/Causes", "causes");
            RELATION_TO_PROPERTY.put("/r/HasSubevent", "hasSubevent");
            RELATION_TO_PROPERTY.put("/r/HasFirstSubevent", "hasFirstSubevent");
            RELATION_TO_PROPERTY.put("/r/HasLastSubevent", "hasLastSubevent");
            RELATION_TO_PROPERTY.put("/r/HasPrerequisite", "hasPrerequisite");
            RELATION_TO_PROPERTY.put("/r/HasProperty", "hasProperty");
            RELATION_TO_PROPERTY.put("/r/MotivatedByGoal", "motivatedByGoal");
            RELATION_TO_PROPERTY.put("/r/ObstructedBy", "obstructedBy");
            RELATION_TO_PROPERTY.put("/r/Desires", "desires");
            RELATION_TO_PROPERTY.put("/r/CreatedBy", "createdBy");
            RELATION_TO_PROPERTY.put("/r/Synonym", "synonym");
            RELATION_TO_PROPERTY.put("/r/Antonym", "antonym");
            RELATION_TO_PROPERTY.put("/r/DistinctFrom", "distinctFrom");
            RELATION_TO_PROPERTY.put("/r/DerivedFrom", "derivedFrom");
            RELATION_TO_PROPERTY.put("/r/SymbolOf", "symbolOf");
            RELATION_TO_PROPERTY.put("/r/DefinedAs", "definedAs");
            RELATION_TO_PROPERTY.put("/r/MannerOf", "mannerOf");
            RELATION_TO_PROPERTY.put("/r/LocatedNear", "locatedNear");
            RELATION_TO_PROPERTY.put("/r/HasContext", "hasContext");
            RELATION_TO_PROPERTY.put("/r/SimilarTo", "similarTo");
            RELATION_TO_PROPERTY.put("/r/EtymologicallyRelatedTo", "etymologicallyRelatedTo");
            RELATION_TO_PROPERTY.put("/r/EtymologicallyDerivedFrom", "etymologicallyDerivedFrom");
            RELATION_TO_PROPERTY.put("/r/CausesDesire", "causesDesire");
            RELATION_TO_PROPERTY.put("/r/MadeOf", "madeOf");
            RELATION_TO_PROPERTY.put("/r/ReceivesAction", "receivesAction");
        }
    }
}