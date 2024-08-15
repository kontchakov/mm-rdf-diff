package uk.ac.bbk.dcs;

import com.google.common.collect.Sets;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RDFDiff {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: RDFDiff <RDF-file-new> <RDF-file-old> [<output-file>]");
            System.exit(-1);
        }

        String filename1 = args[0];
        RDFGraph graph1 = new RDFGraph(filename1);
        System.out.println(graph1.getClasses());
        System.out.println(graph1.getProperties());
        String filename2 = args[1];
        RDFGraph graph2 = new RDFGraph(filename2);
        System.out.println(graph2.getClasses());
        System.out.println(graph2.getProperties());
        PrintStream out = args.length > 2 ? new PrintStream(args[2]) : System.out;

        Map<IRI, Integer> completeClasses = new HashMap<>();
        Map<IRI, Integer> completeProperties = new HashMap<>();
        Map<IRI, Integer> emptyClasses = new HashMap<>();
        Map<IRI, Integer> emptyProperties = new HashMap<>();
        Map<IRI, Integer> newClasses = new HashMap<>();
        Map<IRI, Integer> newProperties = new HashMap<>();

        try {
            out.println(filename1 + " v " + filename2);
            for (IRI c : Sets.union(graph1.getClasses(), graph2.getClasses())) {
                Set<Resource> c1r = graph1.getClassResources(c);
                Set<Resource> c2r = graph2.getClassResources(c);
                Set<Resource> diff1 = Sets.difference(c1r, c2r);
                Set<Resource> diff2 = Sets.difference(c2r, c1r);
                if (diff1.isEmpty() && diff2.isEmpty()) {
                    completeClasses.put(c, c1r.size());
                }
                else if (c1r.isEmpty()) {
                    emptyClasses.put(c, c2r.size());
                }
                else if (c2r.isEmpty()) {
                    newClasses.put(c, c1r.size());
                }
                else {
                    out.println("CLASS " + replacePrefixes(c.toString()) + ": " + c1r.size() + " v " + c2r.size() + " = " + (c1r.size() - c2r.size()) );
                    if (!diff1.isEmpty())
                        out.println("   DIFF1-2 " + renderDiff(diff1, Comparator.comparing(Object::toString)));
                    if (!diff2.isEmpty())
                        out.println("   DIFF2-1 " + renderDiff(diff2, Comparator.comparing(Object::toString)));
                }
            }
            out.println("COMPLETE CLASSES:\n\t" + completeClasses.entrySet().stream()
                    .map(e -> replacePrefixes(e.getKey().toString()) + " (" + e.getValue() + ")")
                    .sorted()
                    .collect(Collectors.joining(",\n\t")));
            out.println("EMPTY CLASSES:\n\t" + emptyClasses.entrySet().stream()
                    .map(e -> replacePrefixes(e.getKey().toString()) + " (" + e.getValue() + ")")
                    .sorted()
                    .collect(Collectors.joining(",\n\t")));
            out.println("NEW CLASSES:\n\t" + newClasses.entrySet().stream()
                    .map(e -> replacePrefixes(e.getKey().toString()) + " (" + e.getValue() + ")")
                    .sorted()
                    .collect(Collectors.joining(",\n\t")));

            for (IRI p : Sets.union(graph1.getProperties(), graph2.getProperties())) {
                Set<Map.Entry<Resource, Value>> p1r = graph1.getPropertyResourcePairs(p);
                Set<Map.Entry<Resource, Value>> p2r = graph2.getPropertyResourcePairs(p);
                Set<Map.Entry<Resource, Value>> diff1 = Sets.difference(p1r, p2r);
                Set<Map.Entry<Resource, Value>> diff2 = Sets.difference(p2r, p1r);
                if (diff1.isEmpty() && diff2.isEmpty()) {
                    completeProperties.put(p, p1r.size());
                }
                else if (p1r.isEmpty()) {
                    emptyProperties.put(p, p2r.size());
                }
                else if (p2r.isEmpty()) {
                    newProperties.put(p, p1r.size());
                }
                else {
                    out.println("PROPERTY " + replacePrefixes(p.toString()) + ": " + p1r.size() + " v " + p2r.size() + " = " + (p1r.size() - p2r.size()));
                    Map<Resource, Value> mapDiff1 = diff1.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                    Map<Resource, Value> mapDiff2 = diff2.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                    Map<Resource, String> sdiff = Sets.intersection(mapDiff1.keySet(), mapDiff2.keySet()).stream()
                            .collect(Collectors.toMap(i -> i, i -> mapDiff1.get(i) + " v " + mapDiff2.get(i)));
                    if (!sdiff.isEmpty())
                        out.println("   DIFF " + renderDiff(sdiff.entrySet(), Comparator.comparing(e -> e.getKey().toString())));

                    Set<Map.Entry<Resource, Value>> sdiff1 = diff1.stream().filter(e -> !mapDiff2.containsKey(e.getKey())).collect(Collectors.toSet());
                    Set<Map.Entry<Resource, Value>> sdiff2 = diff2.stream().filter(e -> !mapDiff1.containsKey(e.getKey())).collect(Collectors.toSet());
                    if (!sdiff1.isEmpty())
                        out.println("   DIFF1-2 " + renderDiff(sdiff1, Comparator.comparing(e -> e.getKey().toString())));
                    if (!sdiff2.isEmpty()) {
                        out.println("   DIFF2-1 " + renderDiff(sdiff2, Comparator.comparing(e -> e.getKey().toString())));
                    }
                }
            }
            out.println("COMPLETE PROPERTIES:\n\t" + completeProperties.entrySet().stream()
                    .map(e -> replacePrefixes(e.getKey().toString()) + " (" + e.getValue() + ")")
                    .sorted()
                    .collect(Collectors.joining(",\n\t")));
            out.println("EMPTY PROPERTIES:\n\t" + emptyProperties.entrySet().stream()
                    .map(e -> replacePrefixes(e.getKey().toString()) + " (" + e.getValue() + ")")
                    .sorted()
                    .collect(Collectors.joining(",\n\t")));
            out.println("NEW PROPERTIES:\n\t" + newProperties.entrySet().stream()
                    .map(e -> replacePrefixes(e.getKey().toString()) + " (" + e.getValue() + ")")
                    .sorted()
                    .collect(Collectors.joining(",\n\t")));
        }
        finally {
            if (out != System.out)
                out.close();
        }
    }
    private static String replacePrefixes(String s) {
        return s.replaceAll("http://mappingMuseums.dcs.bbk.ac.uk/", "mm:");
    }

    private static final int LIMIT = 20_000;

    private static String shorten(String s) {
        if (s.length() > LIMIT)
            s = s.substring(0, LIMIT) + "...";
        return s;
    }

    private static final int SET_LIMIT = 200;
    private static final String INDENT = "\n\t\t\t";

    private static <T> String renderDiff(Set<T> diff, Comparator<T> comparator) {
        return "(" + diff.size() + "):" + INDENT + replacePrefixes(diff.stream()
                .sorted(comparator)
                .limit(SET_LIMIT)
                .map(Object::toString)
                .collect(Collectors.joining(INDENT))) + (diff.size() > SET_LIMIT ? INDENT + "..." : "");
    }
}