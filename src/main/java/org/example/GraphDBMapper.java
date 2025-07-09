package org.example;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.example.models.*;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class GraphDBMapper {
    private final ValueFactory vf;
    private final Repository repo;
    private final RepositoryConnection conn;
    private final List<IndexedWordModel> indexedWords;
    private final List<List<Integer>> relationTriples;
    private final List<Quotation> quotations;
    private final Map<String, String> synsetIdPerLemma;
    private final Map<String, List<String>> relevantWordsPerLemma;
    private final Map<String, List<Edge>> edgesPerLemma;
    private Map<Integer, String> tokenAddresses = new HashMap<>();
    String graphDBServer = "http://localhost:7200";
    String repositoryId = "myrepo";
    String namespace = "http://example.org/";
    Map<String, IRI> propertiesMap, classesMap;

    public GraphDBMapper(Adapter adapter) {
        RemoteRepositoryManager manager = new RemoteRepositoryManager(graphDBServer);
        manager.init();
        repo = manager.getRepository(repositoryId);


        try (RepositoryConnection conn = repo.getConnection()) {
            this.conn = conn;
            System.out.println("Got connection: " + conn);
            conn.clear();
            vf = conn.getValueFactory();

            conn.begin();

            propertiesMap = OntologyProperties.createPropertyMap(vf);
            classesMap = OntologyClasses.createClassMap(vf);

            addStatements();

            relevantWordsPerLemma = adapter.getRelevantWordsPerLemma();
            synsetIdPerLemma = adapter.getSynsetIdPerLemma();

            indexedWords = adapter.getIndexedWordModels();
            for (IndexedWordModel indexedWordModel : indexedWords) {
                mapToRDF(
                        indexedWordModel,
                        relevantWordsPerLemma.get(indexedWordModel.lemma),
                        synsetIdPerLemma.get(indexedWordModel.lemma)
                );
            }
            System.out.println("Mapped indexed words");

            relationTriples = adapter.getRelationTriples();
            for (List<Integer> indices : relationTriples) {
                mapToRDF(indices);
            }
            System.out.println("Mapped relation triples");

            quotations = adapter.getQuotations();
            for (Quotation quotation : quotations) {
                mapToRDF(quotation);
            }
            System.out.println("Mapped quotations");

            edgesPerLemma = adapter.getEdges();
            for (String lemma : edgesPerLemma.keySet()) {
                mapToRDFEdges(edgesPerLemma.get(lemma), lemma);
            }
            System.out.println("Mapped edges");
            conn.commit();

            System.out.println("Mapped everything");

//            exportToTurtle(repo, "src/main/resources/entity.ttl");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String generateIRIAddress(IndexedWordModel indexedWordModel) {
        if (indexedWordModel == null) {
            return null;
        } else {
            return namespace + "token#" + indexedWordModel.hashCode();
        }
    }

    private String generateIRIAddress(IndexedWordModel.GrammaticalRelation relation) {
        if (relation == null) {
            return null;
        } else {
            return namespace + "relation#" + relation.hashCode();
        }
    }

    private String generateIRIAddress(List<Integer> indices) {
        if (indices == null) {
            return null;
        } else {
            return namespace + "relationTriple#" + indices.hashCode();
        }
    }

    private String generateIRIAddress(Quotation quotation) {
        if (quotation == null) {
            return null;
        } else {
            return namespace + "quotation#" + quotation.id;
        }
    }

    private String generateIRIAddress(Edge edge) {
        if (edge == null) {
            return null;
        } else {
            return namespace + "edge#" + edge.hashCode();
        }
    }

    private String generateIRIAddress(Edge.Node node) {
        if (node == null) {
            return null;
        } else {
            return namespace + "node#" + node.hashCode();
        }
    }

    private IndexedWordModel findIndexedWordByIndex(int index) {
        for (IndexedWordModel indexedWordModel : indexedWords) {
            if (indexedWordModel.index == index) {
                return indexedWordModel;
            }
        }

        return null;
    }

    public void mapToRDFEdges(List<Edge> edges, String lemma) {
        if (edges != null) {
            boolean isThereEdgeWithNoTranslation = edges
                    .stream()
                    .anyMatch(edge -> Objects.equals(edge.start.language, edge.end.language));

            edges = isThereEdgeWithNoTranslation
                    ? edges.stream()
                        .filter(edge -> Objects.equals(edge.start.language, edge.end.language))
                        .collect(Collectors.toList())
                    : edges;

            for (Edge edge : edges) {
                IRI edgeIRI = vf.createIRI(generateIRIAddress(edge));
                IRI nodeStart = vf.createIRI(generateIRIAddress(edge.start));
                IRI nodeEnd = vf.createIRI(generateIRIAddress(edge.end));

                conn.add(edgeIRI, RDF.TYPE, classesMap.get("Edge"));

                conn.add(edgeIRI, propertiesMap.get("forLemma"), vf.createLiteral(lemma));
                conn.add(edgeIRI, propertiesMap.get("hasStart"), nodeStart);
                conn.add(edgeIRI, propertiesMap.get("hasEnd"), nodeEnd);
                conn.add(edgeIRI, propertiesMap.get("hasRelationWeight"), vf.createLiteral(edge.weight));

                conn.add(nodeStart, propertiesMap.get("hasLabel"), vf.createLiteral(edge.start.label));
                conn.add(nodeEnd, propertiesMap.get("hasLabel"), vf.createLiteral(edge.end.label));
                conn.add(nodeStart, propertiesMap.getOrDefault(edge.relation.id, propertiesMap.get("/r/RelatedTo")), nodeEnd);
            }
        }
    }

    public void mapToRDF(IndexedWordModel word, List<String> relevantWords, String synsetId) {
        String address = generateIRIAddress(word);
        tokenAddresses.put(word.index, address);

        IRI token = vf.createIRI(address);
        conn.add(token, RDF.TYPE, (word.posTag.contains("VB") ? classesMap.get("Verb") : classesMap.get("Token")));

        conn.add(token, propertiesMap.get("hasLemma"), vf.createLiteral(word.lemma));
        conn.add(token, propertiesMap.get("hasPartOfSpeech"), vf.createLiteral(word.posTag));
        conn.add(token, propertiesMap.get("word"), vf.createLiteral(word.word));
        conn.add(token, propertiesMap.get("hasIndex"), vf.createLiteral(word.index));

        if (word.ner != null) {
            conn.add(token, propertiesMap.get("hasNamedEntityRecognition"), vf.createLiteral(word.ner));
        }

        if (synsetId != null) {
            conn.add(token, propertiesMap.get("hasSynsetId"), vf.createLiteral(synsetId));
        }

        if (relevantWords != null) {
            for (String relevantWord : relevantWords) {
                conn.add(token, propertiesMap.get("hasRelevantWord"), vf.createLiteral(relevantWord));
            }
        }

        if (word.mentionId != null) {
            conn.add(token, propertiesMap.get("hasChainMention"), vf.createLiteral(word.chainMentionId));
            conn.add(token, propertiesMap.get("hasMention"), vf.createLiteral(word.mentionId));
        }

        if (word.relations != null) {
            for (IndexedWordModel.GrammaticalRelation relation : word.relations) {
                for (int targetIndex : relation.targetIndices) {
                    IndexedWordModel target = findIndexedWordByIndex(targetIndex);
                    if (target != null) {
                        IRI relationIRI = vf.createIRI(generateIRIAddress(relation));

                        conn.add(relationIRI, RDF.TYPE, classesMap.get("GrammaticalRelation"));
                        conn.add(relationIRI, propertiesMap.get("target"), vf.createIRI(generateIRIAddress(target)));
                        conn.add(relationIRI, propertiesMap.get("relation"), vf.createLiteral(relation.grammaticalRelation));
                        conn.add(token, propertiesMap.get("hasGrammaticalRelation"), relationIRI);
                    }

                }
            }
        }
    }

    public void mapToRDF(Quotation quotation) {
        IRI quotationIRI = vf.createIRI(generateIRIAddress(quotation));
        conn.add(quotationIRI, RDF.TYPE, classesMap.get("Quotation"));
        if (quotation.speaker != null) {
            conn.add(quotationIRI, propertiesMap.get("hasSpeaker"), vf.createLiteral(quotation.speaker));
        }
        conn.add(quotationIRI, propertiesMap.get("hasText"), vf.createLiteral(quotation.quote));
        for (IndexedWordModel word : quotation.indexedWords) {
            conn.add(quotationIRI, propertiesMap.get("hasToken"), vf.createIRI(generateIRIAddress(word)));
        }
    }

    public void mapToRDF(List<Integer> indices) {
        String address = generateIRIAddress(indices);
        IRI relationTriple = vf.createIRI(address);
        conn.add(relationTriple, RDF.TYPE, classesMap.get("RelationTriple"));

        indices.forEach(index -> {
            IRI token = vf.createIRI(tokenAddresses.get(index));
            conn.add(token, propertiesMap.get("isInRelationTriple"), relationTriple);
        });
    }

    public void addStatements() {
        conn.add(classesMap.get("Token"), RDF.TYPE, OWL.CLASS);
        conn.add(classesMap.get("Token"), RDFS.LABEL, vf.createLiteral("Text token"));

        conn.add(classesMap.get("Verb"), RDF.TYPE, OWL.CLASS);
        conn.add(classesMap.get("Verb"), RDFS.SUBCLASSOF, classesMap.get("Token"));
        conn.add(classesMap.get("Verb"), RDFS.LABEL, vf.createLiteral("Verb"));

        conn.add(classesMap.get("ThematicRole"), RDF.TYPE, OWL.CLASS);
        conn.add(classesMap.get("ThematicRole"), RDFS.LABEL, vf.createLiteral("Thematic role"));

        conn.add(classesMap.get("Predicate"), RDF.TYPE, OWL.CLASS);
        conn.add(classesMap.get("Predicate"), RDFS.LABEL, vf.createLiteral("Verb Predicate"));

        conn.add(classesMap.get("SemanticArgument"), RDF.TYPE, OWL.CLASS);
        conn.add(classesMap.get("SemanticArgument"), RDFS.LABEL, vf.createLiteral("Semantic Argument"));

        conn.add(classesMap.get("GrammaticalRelation"), RDF.TYPE, OWL.CLASS);
        conn.add(classesMap.get("GrammaticalRelation"), RDFS.LABEL, vf.createLiteral("Grammatical Relation"));

        conn.add(classesMap.get("Synset"), RDF.TYPE, OWL.CLASS);
        conn.add(classesMap.get("Synset"), RDFS.LABEL, vf.createLiteral("Synset"));

        conn.add(classesMap.get("RelationTriple"), RDF.TYPE, OWL.CLASS);
        conn.add(classesMap.get("RelationTriple"), RDFS.LABEL, vf.createLiteral("Relation Triple"));

        conn.add(classesMap.get("Quotation"), RDF.TYPE, OWL.CLASS);
        conn.add(classesMap.get("Quotation"), RDFS.LABEL, vf.createLiteral("Quotation"));

        conn.add(classesMap.get("Edge"), RDF.TYPE, OWL.CLASS);
        conn.add(classesMap.get("Edge"), RDFS.LABEL, vf.createLiteral("Edge"));

        conn.add(classesMap.get("Node"), RDF.TYPE, OWL.CLASS);
        conn.add(classesMap.get("Node"), RDFS.LABEL, vf.createLiteral("Node"));

        // Add property definitions
        conn.add(propertiesMap.get("hasThematicRole"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("hasThematicRole"), RDFS.DOMAIN, classesMap.get("Verb"));
        conn.add(propertiesMap.get("hasThematicRole"), RDFS.RANGE, classesMap.get("ThematicRole"));

        conn.add(propertiesMap.get("hasLemma"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("hasLemma"), RDFS.DOMAIN, classesMap.get("Token"));
        conn.add(propertiesMap.get("hasLemma"), RDFS.RANGE, XSD.STRING);

        conn.add(propertiesMap.get("word"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("word"), RDFS.DOMAIN, classesMap.get("Token"));
        conn.add(propertiesMap.get("word"), RDFS.RANGE, XSD.STRING);

        conn.add(propertiesMap.get("hasPartOfSpeech"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("hasPartOfSpeech"), RDFS.DOMAIN, classesMap.get("Token"));
        conn.add(propertiesMap.get("hasPartOfSpeech"), RDFS.RANGE, XSD.STRING);

        conn.add(propertiesMap.get("hasNamedEntityRecognition"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("hasNamedEntityRecognition"), RDFS.DOMAIN, classesMap.get("Token"));
        conn.add(propertiesMap.get("hasNamedEntityRecognition"), RDFS.RANGE, XSD.STRING);

        conn.add(propertiesMap.get("hasChainMention"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("hasChainMention"), RDFS.DOMAIN, classesMap.get("Token"));
        conn.add(propertiesMap.get("hasChainMention"), RDFS.RANGE, XSD.INTEGER);

        conn.add(propertiesMap.get("hasMention"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("hasMention"), RDFS.DOMAIN, classesMap.get("Token"));
        conn.add(propertiesMap.get("hasMention"), RDFS.RANGE, XSD.INTEGER);

        //Synset
        conn.add(propertiesMap.get("hasSynsetId"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("hasSynsetId"), RDFS.DOMAIN, classesMap.get("Token"));
        conn.add(propertiesMap.get("hasSynsetId"), RDFS.RANGE, XSD.STRING);

        conn.add(propertiesMap.get("hasRelevantWord"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("hasRelevantWord"), RDFS.DOMAIN, classesMap.get("Token"));
        conn.add(propertiesMap.get("hasRelevantWord"), RDFS.RANGE, XSD.STRING);

        //ConceptNet
        conn.add(propertiesMap.get("relatedToCN"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("relatedToCN"), RDFS.DOMAIN, classesMap.get("Token"));
        conn.add(propertiesMap.get("relatedToCN"), RDFS.RANGE, XSD.STRING);

        for (String property: OntologyProperties.ConceptNetRDFProperties.RELATION_TO_PROPERTY.keySet()) {
            conn.add(propertiesMap.get(property), RDF.TYPE, RDF.PROPERTY);
            conn.add(propertiesMap.get(property), RDFS.SUBPROPERTYOF, propertiesMap.get("relatedToCN"));
        }

        conn.add(propertiesMap.get("forLemma"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("forLemma"), RDFS.DOMAIN, classesMap.get("Edge"));
        conn.add(propertiesMap.get("forLemma"), RDFS.RANGE, XSD.STRING);

        conn.add(propertiesMap.get("hasStart"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("hasStart"), RDFS.DOMAIN, classesMap.get("Edge"));
        conn.add(propertiesMap.get("hasStart"), RDFS.RANGE, classesMap.get("Node"));

        conn.add(propertiesMap.get("hasEnd"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("hasEnd"), RDFS.DOMAIN, classesMap.get("Edge"));
        conn.add(propertiesMap.get("hasEnd"), RDFS.RANGE, classesMap.get("Node"));

        conn.add(propertiesMap.get("hasRelationWeight"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("hasRelationWeight"), RDFS.DOMAIN, classesMap.get("Edge"));
        conn.add(propertiesMap.get("hasRelationWeight"), RDFS.RANGE, XSD.INTEGER);

        conn.add(propertiesMap.get("hasLabel"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("hasLabel"), RDFS.DOMAIN, classesMap.get("Node"));
        conn.add(propertiesMap.get("hasLabel"), RDFS.RANGE, XSD.STRING);

        conn.add(propertiesMap.get("hasRole"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("hasRole"), RDFS.DOMAIN, classesMap.get("Token"));
        conn.add(propertiesMap.get("hasRole"), RDFS.RANGE, classesMap.get("ThematicRole"));

        conn.add(propertiesMap.get("thematicType"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("thematicType"), RDFS.DOMAIN, classesMap.get("ThematicRole"));
        conn.add(propertiesMap.get("thematicType"), RDFS.RANGE, XSD.STRING);

//        conn.add(propertiesMap.get("token"), RDF.TYPE, RDF.PROPERTY);
//        conn.add(propertiesMap.get("token"), RDFS.DOMAIN, classesMap.get("ThematicRole"));
//        conn.add(propertiesMap.get("token"), RDFS.RANGE, classesMap.get("Token"));

        conn.add(propertiesMap.get("hasPredicate"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("hasPredicate"), RDFS.DOMAIN, classesMap.get("Verb"));
        conn.add(propertiesMap.get("hasPredicate"), RDFS.RANGE, classesMap.get("Predicate"));

        conn.add(propertiesMap.get("value"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("value"), RDFS.DOMAIN, classesMap.get("Predicate"));
        conn.add(propertiesMap.get("value"), RDFS.RANGE, XSD.STRING);

        conn.add(propertiesMap.get("argument"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("argument"), RDFS.DOMAIN, classesMap.get("Predicate"));
        conn.add(propertiesMap.get("argument"), RDFS.RANGE, classesMap.get("SemanticArgument"));

        conn.add(propertiesMap.get("argumentType"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("argumentType"), RDFS.DOMAIN, classesMap.get("SemanticArgument"));
        conn.add(propertiesMap.get("argumentType"), RDFS.RANGE, XSD.STRING);

        conn.add(propertiesMap.get("argumentValue"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("argumentValue"), RDFS.DOMAIN, classesMap.get("SemanticArgument"));
        conn.add(propertiesMap.get("argumentValue"), RDFS.RANGE, XSD.STRING);

        conn.add(propertiesMap.get("hasGrammaticalRelation"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("hasGrammaticalRelation"), RDFS.DOMAIN, classesMap.get("Token"));
        conn.add(propertiesMap.get("hasGrammaticalRelation"), RDFS.RANGE, classesMap.get("GrammaticalRelation"));

        conn.add(propertiesMap.get("target"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("target"), RDFS.DOMAIN, classesMap.get("GrammaticalRelation"));
        conn.add(propertiesMap.get("target"), RDFS.RANGE, classesMap.get("Token"));

        conn.add(propertiesMap.get("relation"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("relation"), RDFS.DOMAIN, classesMap.get("GrammaticalRelation"));
        conn.add(propertiesMap.get("relation"), RDFS.RANGE, XSD.STRING);

        conn.add(propertiesMap.get("hasIndex"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("hasIndex"), RDFS.DOMAIN, classesMap.get("Token"));
        conn.add(propertiesMap.get("hasIndex"), RDFS.RANGE, XSD.INTEGER);

        conn.add(propertiesMap.get("isInRelationTriple"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("isInRelationTriple"), RDFS.DOMAIN, classesMap.get("Token"));
        conn.add(propertiesMap.get("isInRelationTriple"), RDFS.RANGE, classesMap.get("RelationTriple"));

        conn.add(propertiesMap.get("hasToken"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("hasToken"), RDFS.DOMAIN, classesMap.get("Quotation"));
        conn.add(propertiesMap.get("hasToken"), RDFS.DOMAIN, classesMap.get("Token"));

        conn.add(propertiesMap.get("hasSpeaker"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("hasSpeaker"), RDFS.DOMAIN, classesMap.get("Quotation"));
        conn.add(propertiesMap.get("hasSpeaker"), RDFS.RANGE, XSD.STRING);

        conn.add(propertiesMap.get("hasText"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("hasText"), RDFS.DOMAIN, classesMap.get("Quotation"));
        conn.add(propertiesMap.get("hasText"), RDFS.RANGE, XSD.STRING);

    }

    public static void exportToTurtle(Repository repository, String outputPath) throws Exception {
        try (RepositoryConnection conn = repository.getConnection();
             FileOutputStream out = new FileOutputStream(outputPath)) {

            // Export all statements in Turtle format
            conn.export(Rio.createWriter(RDFFormat.TURTLE, out));

            System.out.println("Successfully exported to: " + outputPath);
        }
    }
}