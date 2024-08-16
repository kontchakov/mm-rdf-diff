package uk.ac.bbk.dcs;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RDFExtract {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: RDFExtract <RDF-file> <property-IRI> [<output-file>]");
            System.exit(-1);
        }

        String filename1 = args[0];
        RDFGraph graph1 = new RDFGraph(filename1);
        System.out.println(graph1.getClasses());
        System.out.println(graph1.getProperties());

        IRI iri = Values.iri(args[1]);
        PrintStream out = args.length > 2 ? new PrintStream(args[2]) : System.out;

        try {
            Set<Map.Entry<Resource, Value>> inst = graph1.getPropertyResourcePairs(iri);
            Map<Resource, List<Map.Entry<Resource, Value>>> map = inst.stream().collect(Collectors.groupingBy(Map.Entry::getKey));
            out.println(map.entrySet().stream()
                    .sorted(Comparator.comparing(e -> e.getKey().toString()))
                    .map(e -> e.getKey() + ", " + e.getValue().stream().map(Map.Entry::getValue).map(Object::toString).sorted().collect(Collectors.joining(", ")))
                    .map(RDFExtract::replacePrefixes)
                    .collect(Collectors.joining("\n")));
        }
        finally {
            if (out != System.out)
                out.close();
        }
    }

    private static String replacePrefixes(String s) {
        return s.replaceAll("http://mappingMuseums.dcs.bbk.ac.uk/", "mm:");
    }
}
