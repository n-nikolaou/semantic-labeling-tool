package org.example;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.example.models.IndexedWordModel;
import org.example.models.SemanticArgument;
import org.example.models.VerbDetails;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GraphDBMapper {
    private final ValueFactory vf;
    private final Repository repo;
    private final RepositoryConnection conn;
    private final ArrayList<IndexedWordModel> indexedWords;
    String graphDBServer = "http://localhost:7200";
    String repositoryId = "myrepo";
    String namespace = "http://example.org/";
    Map<String, IRI> propertiesMap, classesMap;

    public GraphDBMapper(VerbNetAdapter adapter) {
        RemoteRepositoryManager manager = new RemoteRepositoryManager(graphDBServer);
        manager.init();
        repo = manager.getRepository(repositoryId);


        try (RepositoryConnection conn = repo.getConnection()) {
            this.conn = conn;
            conn.clear();
            vf = conn.getValueFactory();

            propertiesMap = OntologyProperties.createPropertyMap(vf);
            classesMap = OntologyClasses.createClassMap(vf);

            addStatements();

            indexedWords = adapter.getIndexedWordModels();
            for (IndexedWordModel indexedWordModel : indexedWords) {
                mapToRDF(indexedWordModel);
            }

//            exportToTurtle(repo, "src/main/resources/entity.ttl");
//            System.out.println("Done!");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String generateIRIAddress(IndexedWordModel indexedWordModel) {
        if (indexedWordModel == null) {
            return null;
        } else {
            return namespace +
                    ((indexedWordModel.verbDetails == null)
                    ? "token#"
                    : "verb#") +
                    indexedWordModel.hashCode();
        }
    }

    private String generateIRIAddress(VerbDetails.ThematicRole thematicRole) {
        if (thematicRole == null) {
            return null;
        } else {
            return namespace + "role#" + thematicRole.hashCode();
        }
    }

    private String generateIRIAddress(SemanticArgument semanticArgument) {
        if (semanticArgument == null) {
            return null;
        } else {
            return namespace + "predicate#" + semanticArgument.hashCode();
        }
    }

    private String generateIRIAddress(SemanticArgument.Argument argument) {
        if (argument == null) {
            return null;
        } else {
            return namespace + "argument#" + argument.hashCode();
        }
    }

    private String generateIRIAddress(IndexedWordModel.GrammaticalRelation relation) {
        if (relation == null) {
            return null;
        } else {
            return namespace + "relation#" + relation.hashCode();
        }
    }

    private String generateIRIAddress(String entity) {
        if (entity == null) {
            return null;
        } else {
            return namespace + entity;
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

    public void mapToRDF(IndexedWordModel word) {
        IRI token = vf.createIRI(generateIRIAddress(word));
        conn.add(token, RDF.TYPE, (word.verbDetails == null ? classesMap.get("Token") : classesMap.get("Verb")));

        conn.add(token, propertiesMap.get("hasLemma"), vf.createLiteral(word.lemma));
        conn.add(token, propertiesMap.get("hasPartOfSpeech"), vf.createLiteral(word.posTag));
        conn.add(token, propertiesMap.get("word"), vf.createLiteral(word.word));
        conn.add(token, propertiesMap.get("hasIndex"), vf.createLiteral(word.index));

        if (word.ner != null) {
            conn.add(token, propertiesMap.get("hasNamedEntityRecognition"), vf.createLiteral(word.ner));
        }

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

        if (word.verbDetails != null) {
            for (VerbDetails.ThematicRole thematicRole : word.verbDetails.thematicRoles) {
                if (thematicRole.type != null) {
                    IRI roleIRI = vf.createIRI(generateIRIAddress(thematicRole));
                    IndexedWordModel target = findIndexedWordByIndex(thematicRole.wordIndex);

                    conn.add(roleIRI, RDF.TYPE, classesMap.get("ThematicRole"));
                    conn.add(roleIRI, propertiesMap.get("thematicType"), vf.createLiteral(thematicRole.type));
                    conn.add(roleIRI, propertiesMap.get("token"), vf.createIRI(generateIRIAddress(target)));
                    conn.add(token, propertiesMap.get("hasThematicRole"), roleIRI);
                }
            }

            for (SemanticArgument semanticArgument : word.verbDetails.semanticArguments) {
                IRI predicateIRI = vf.createIRI(generateIRIAddress(semanticArgument));
                Literal predicateLiteral = vf.createLiteral(semanticArgument.predicate);

                conn.add(predicateIRI, RDF.TYPE, classesMap.get("Predicate"));
                for (SemanticArgument.Argument argument : semanticArgument.arguments) {
                    IRI argumentIRI = vf.createIRI(generateIRIAddress(argument));
                    Literal argumentValueLiteral = vf.createLiteral(argument.value);
                    Literal argumentTypeLiteral = vf.createLiteral(argument.type);

                    conn.add(argumentIRI, RDF.TYPE, classesMap.get("SemanticArgument"));
                    conn.add(argumentIRI, propertiesMap.get("argumentValue"), argumentValueLiteral);
                    conn.add(argumentIRI, propertiesMap.get("argumentType"), argumentTypeLiteral);
                    conn.add(predicateIRI, propertiesMap.get("argument"), argumentIRI);
                }

                conn.add(predicateIRI, propertiesMap.get("value"), predicateLiteral);
                conn.add(token, propertiesMap.get("hasPredicate"), predicateIRI);
            }

        }
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

        conn.add(classesMap.get("GrammaticalRelation"), RDF.TYPE, RDFS.CLASS);
        conn.add(classesMap.get("GrammaticalRelation"), RDFS.LABEL, vf.createLiteral("Grammatical Relation"));

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

        conn.add(propertiesMap.get("hasRole"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("hasRole"), RDFS.DOMAIN, classesMap.get("Token"));
        conn.add(propertiesMap.get("hasRole"), RDFS.RANGE, classesMap.get("ThematicRole"));

        conn.add(propertiesMap.get("thematicType"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("thematicType"), RDFS.DOMAIN, classesMap.get("ThematicRole"));
        conn.add(propertiesMap.get("thematicType"), RDFS.RANGE, XSD.STRING);

        conn.add(propertiesMap.get("token"), RDF.TYPE, RDF.PROPERTY);
        conn.add(propertiesMap.get("token"), RDFS.DOMAIN, classesMap.get("ThematicRole"));
        conn.add(propertiesMap.get("token"), RDFS.RANGE, classesMap.get("Token"));

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