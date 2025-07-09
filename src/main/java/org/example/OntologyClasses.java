package org.example;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;

public class OntologyClasses {

    private static final String BASE = "http://example.org/";

    public static Map<String, IRI> createClassMap(ValueFactory vf) {
        Map<String, IRI> classMap = new HashMap<>();

        classMap.put("Token", vf.createIRI(BASE, "Token"));
        classMap.put("Verb", vf.createIRI(BASE, "Verb"));
        classMap.put("ThematicRole", vf.createIRI(BASE, "ThematicRole"));
        classMap.put("Predicate", vf.createIRI(BASE, "Predicate"));
        classMap.put("SemanticArgument", vf.createIRI(BASE, "SemanticArgument"));
        classMap.put("GrammaticalRelation", vf.createIRI(BASE, "GrammaticalRelation"));
        classMap.put("Synset", vf.createIRI(BASE, "Synset"));
        classMap.put("Edge", vf.createIRI(BASE, "Edge"));
        classMap.put("Node", vf.createIRI(BASE, "Node"));
        classMap.put("RelationTriple", vf.createIRI(BASE, "RelationTriple"));
        classMap.put("Quotation", vf.createIRI(BASE, "Quotation"));

        return classMap;
    }
}
