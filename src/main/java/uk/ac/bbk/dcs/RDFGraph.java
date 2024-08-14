package uk.ac.bbk.dcs;

import com.google.common.collect.Streams;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RDFGraph {

    private final Map<IRI, Set<Resource>> classAssertions = new HashMap<>();
    private final Map<IRI, Set<Map.Entry<Resource, Value>>> propertyAssertions = new HashMap<>();
    private final Map<IRI, Map<Resource, Value>> blankNodesMaps;

    public RDFGraph(String filename) throws IOException {
        InputStream input = new FileInputStream(filename);
        RDFParser parser = Rio.createParser(RDFFormat.RDFXML);
        parser.setPreserveBNodeIDs(true);
        Model model = new LinkedHashModel();
        parser.setRDFHandler(new StatementCollector(model));
        parser.parse(input);
        int count = 0;
        for (Statement st : model) {
            if (st.getPredicate().equals(RDF.TYPE)) {
                classAssertions.computeIfAbsent((IRI)st.getObject(), p -> new HashSet<>())
                        .add(st.getSubject());
            }
            else {
                propertyAssertions.computeIfAbsent(st.getPredicate(), p -> new HashSet<>())
                        .add(Map.entry(st.getSubject(), st.getObject()));
            }
/*
            if (st.getObject() instanceof BNode) {
                for (Statement st2 : model) {
                    if (st2.getSubject() == st.getObject() && count++ < 20)
                        System.out.println(st2);
                }
            }
 */
        }

        blankNodesMaps = Stream.of("http://www.w3.org/2001/XMLSchema#string",
                "http://www.w3.org/2001/XMLSchema#anyType",
                "http://www.w3.org/2001/XMLSchema#int")
                .map(s -> Values.iri(s))
                .map(i -> Map.entry(i, propertyAssertions.getOrDefault(i, Set.of()).stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // RESOLVE BLANK NODES
        for (IRI p : propertyAssertions.keySet()) {
            Set<Map.Entry<Resource, Value>> fixed = propertyAssertions.get(p).stream()
                    .map(this::resolveBlankNode)
                    .collect(Collectors.toSet());
            propertyAssertions.put(p, fixed);
        }
    }

    private Map.Entry<Resource, Value> resolveBlankNode(Map.Entry<Resource, Value> e) {
        return e.getValue() instanceof BNode
                ? Map.entry(e.getKey(), resolveBlankNode(e.getValue()))
                : e;
    }

    private Value resolveBlankNode(Value v) {
        return blankNodesMaps.entrySet().stream()
                .filter(m -> m.getValue().containsKey(v))
                .<Value>map(m -> Values.literal(m.getValue().get(v).stringValue(), m.getKey()))
                .findAny()
                .orElse(v);
    }

    public Set<IRI> getClasses() {
        return classAssertions.keySet();
    }

    public Set<IRI> getProperties() {
        return propertyAssertions.keySet();
    }

    public Set<Resource> getClassResources(IRI clazz) {
        return Optional.ofNullable(classAssertions.get(clazz)).orElseGet(Set::of);
    }

    public Set<Map.Entry<Resource, Value>> getPropertyResourcePairs(IRI property) {
        return Optional.ofNullable(propertyAssertions.get(property)).orElseGet(Set::of);
    }
}
