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
        propertyMap.put("hasRole", vf.createIRI(BASE, "hasRole"));
        propertyMap.put("hasThematicRole", vf.createIRI(BASE, "hasThematicRole"));
        propertyMap.put("thematicType", vf.createIRI(BASE, "thematicType"));
        propertyMap.put("token", vf.createIRI(BASE, "token"));
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

        return propertyMap;
    }
}