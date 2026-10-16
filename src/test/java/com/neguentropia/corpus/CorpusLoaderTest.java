package com.neguentropia.corpus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

class CorpusLoaderTest {
    @TempDir Path directory;
    private final CorpusLoader loader = new CorpusLoader(SyntheticManifest.JSON);

    @Test void preservesAllTwentyOriginalsAndNeverReadsSourceFiles() throws Exception {
        var input = SyntheticManifest.create();
        var loaded = loader.load(SyntheticManifest.write(directory, input));
        assertEquals(20, loaded.papers().size());
        assertEquals(SyntheticManifest.papers(input).getFirst().get("originalAbstract"),
                loaded.papers().getFirst().originalAbstract());
        assertFalse(loaded.humanApproved());
        assertEquals("paper-20", loaded.papers().getLast().id());
    }

    record InvalidCase(String name, Consumer<Map<String, Object>> mutate) {
        @Override public String toString() { return name; }
    }
    static Stream<InvalidCase> invalidCases() {
        return Stream.of(
            new InvalidCase("nineteen records", m -> SyntheticManifest.papers(m).removeLast()),
            new InvalidCase("twenty-one records", m -> SyntheticManifest.papers(m).add(SyntheticManifest.papers(m).getFirst())),
            new InvalidCase("duplicate id", m -> SyntheticManifest.papers(m).getLast().put("id", "paper-01")),
            new InvalidCase("unstable order", m -> Collections.swap(SyntheticManifest.papers(m), 0, 1)),
            new InvalidCase("blank title", m -> SyntheticManifest.papers(m).getFirst().put("title", "  ")),
            new InvalidCase("missing authors", m -> SyntheticManifest.papers(m).getFirst().put("authors", List.of())),
            new InvalidCase("blank author", m -> SyntheticManifest.papers(m).getFirst().put("authors", List.of(" "))),
            new InvalidCase("blank abstract", m -> SyntheticManifest.papers(m).getFirst().put("originalAbstract", " ")),
            new InvalidCase("unusable abstract", m -> SyntheticManifest.papers(m).getFirst().put("originalAbstract", "123 😀")),
            new InvalidCase("wrong text hash", m -> SyntheticManifest.papers(m).getFirst().put("abstractSha256", "0".repeat(64))),
            new InvalidCase("invalid source hash", m -> SyntheticManifest.papers(m).getFirst().put("sourceSha256", "xyz")),
            new InvalidCase("duplicate source hash", m -> SyntheticManifest.papers(m).getLast().put("sourceSha256", SyntheticManifest.papers(m).getFirst().get("sourceSha256"))),
            new InvalidCase("duplicate source reference", m -> SyntheticManifest.papers(m).getLast().put("sourceFile", SyntheticManifest.papers(m).getFirst().get("sourceFile"))),
            new InvalidCase("duplicate abstract", m -> SyntheticManifest.papers(m).getLast().put("originalAbstract", SyntheticManifest.papers(m).getFirst().get("originalAbstract"))),
            new InvalidCase("blank source reference", m -> SyntheticManifest.papers(m).getFirst().put("sourceFile", "")),
            new InvalidCase("wrong schema version", m -> m.put("schemaVersion", 2)),
            new InvalidCase("wrong corpus version", m -> m.put("corpusVersion", "abstracts-sha256-" + "0".repeat(64))),
            new InvalidCase("unreviewed corpus", m -> m.put("reviewStatus", "candidate")),
            new InvalidCase("missing corpus approval flag", m -> m.remove("humanApproved")),
            new InvalidCase("changed corpus approval flag", m -> m.put("humanApproved", true)),
            new InvalidCase("unreviewed paper", m -> SyntheticManifest.papers(m).getFirst().put("reviewStatus", "candidate")),
            new InvalidCase("changed paper approval flag", m -> SyntheticManifest.papers(m).getFirst().put("humanApproved", true)),
            new InvalidCase("missing paper approval flag", m -> SyntheticManifest.papers(m).getFirst().remove("humanApproved"))
        );
    }

    @ParameterizedTest(name = "{0}") @MethodSource("invalidCases")
    void rejectsInvalidManifestAtomically(InvalidCase testCase) throws Exception {
        var input = SyntheticManifest.create();
        testCase.mutate().accept(input);
        if (!testCase.name().equals("wrong text hash")) {
            for (var paper : SyntheticManifest.papers(input)) {
                paper.put("abstractSha256", SyntheticManifest.hash((String) paper.get("originalAbstract")));
            }
        }
        if (!testCase.name().equals("wrong corpus version")) SyntheticManifest.version(input);
        assertThrows(Exception.class, () -> loader.load(SyntheticManifest.write(directory, input)));
    }

    @Test void versionMatchesCompactJsonStringifyEscapingWithoutChangingOriginal() throws Exception {
        var input = SyntheticManifest.create();
        var first = SyntheticManifest.papers(input).getFirst();
        String original = first.get("originalAbstract") + " quotes\" backslash\\ tab\t return\r back\b form\f control\u001f separators\u2028\u2029";
        first.put("originalAbstract", original);
        first.put("abstractSha256", SyntheticManifest.hash(original));
        var tuples = SyntheticManifest.papers(input).stream().map(p -> List.of(p.get("id"), p.get("title"),
                p.get("authors"), p.get("originalAbstract"), p.get("sourceSha256"))).toList();
        String compact = SyntheticManifest.JSON.writeValueAsString(tuples).replace("\\u001F", "\\u001f");
        input.put("corpusVersion", "abstracts-sha256-" + SyntheticManifest.hash(compact));
        var loaded = loader.load(SyntheticManifest.write(directory, input));
        assertEquals(original, loaded.papers().getFirst().originalAbstract());
    }

    @Test void rejectsEscapedUnpairedSurrogateAfterParsing() throws Exception {
        String json = SyntheticManifest.JSON.writeValueAsString(SyntheticManifest.create())
                .replace("synthetic1 ", "synthetic1 \\ud800 ");
        var file = Files.writeString(directory.resolve("escaped-surrogate.json"), json);
        assertThrows(Exception.class, () -> loader.load(file));
    }

    @Test void rejectsDuplicateKeysAndTrailingJson() throws Exception {
        String json = SyntheticManifest.JSON.writeValueAsString(SyntheticManifest.create());
        for (String invalid : List.of(json + " {}", json.replace("\"schemaVersion\":1", "\"schemaVersion\":1,\"schemaVersion\":1"))) {
            var file = Files.writeString(directory.resolve("ambiguous.json"), invalid);
            assertThrows(Exception.class, () -> loader.load(file));
        }
    }

    @Test void rejectsMalformedUtf8() throws Exception {
        var file = Files.write(directory.resolve("malformed.json"), new byte[]{(byte) 0xc3, 0x28});
        assertThrows(Exception.class, () -> loader.load(file));
    }

    @Test void rejectsUnknownFields() throws Exception {
        var input = SyntheticManifest.create();
        input.put("unexpected", true);
        assertThrows(Exception.class, () -> loader.load(SyntheticManifest.write(directory, input)));
    }

    @Test void rejectsWrongJsonTypesInsteadOfCoercingThem() throws Exception {
        for (Object invalid : List.of("1", 4294967297L, 1.5)) {
            var input = SyntheticManifest.create();
            input.put("schemaVersion", invalid);
            assertThrows(Exception.class, () -> loader.load(SyntheticManifest.write(directory, input)));
        }
        var input = SyntheticManifest.create();
        SyntheticManifest.papers(input).getFirst().put("title", 123);
        assertThrows(Exception.class, () -> loader.load(SyntheticManifest.write(directory, input)));
    }
}
