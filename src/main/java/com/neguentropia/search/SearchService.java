package com.neguentropia.search;

import com.neguentropia.corpus.CorpusLoader.Paper;
import com.neguentropia.corpus.CorpusService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SearchService {
    private final CorpusService corpus;

    public SearchService(CorpusService corpus) {
        this.corpus = corpus;
    }

    public boolean ready() {
        return corpus.ready();
    }

    public SearchResponse search(String pattern, List<String> methods) {
        List<SearchResult> results = new ArrayList<>();
        for (String method : methods) {
            if (method.equals("kmp")) {
                results.add(searchWithKmp(pattern));
            } else if (method.equals("boyer-moore")) {
                results.add(searchWithBoyerMoore(pattern));
            } else {
                results.add(searchWithRabinKarp(pattern));
            }
        }
        return new SearchResponse(pattern, results);
    }

    private SearchResult searchWithKmp(String pattern) {
        long preparationStart = System.nanoTime();
        int[] failure = KnuthMorrisPratt.prepare(pattern);
        long searchStart = System.nanoTime();
        List<PaperSearchResult> papers = new ArrayList<>();
        for (Paper paper : corpus.papers()) {
            List<Integer> positions = KnuthMorrisPratt.search(paper.originalAbstract(), pattern, failure);
            papers.add(new PaperSearchResult(paper.id(), positions));
        }
        long searchEnd = System.nanoTime();
        return new SearchResult("kmp", milliseconds(preparationStart, searchStart),
                milliseconds(searchStart, searchEnd), papers);
    }

    private SearchResult searchWithBoyerMoore(String pattern) {
        long preparationStart = System.nanoTime();
        Map<Character, Integer> last = BoyerMoore.prepare(pattern);
        long searchStart = System.nanoTime();
        List<PaperSearchResult> papers = new ArrayList<>();
        for (Paper paper : corpus.papers()) {
            List<Integer> positions = BoyerMoore.search(paper.originalAbstract(), pattern, last);
            papers.add(new PaperSearchResult(paper.id(), positions));
        }
        long searchEnd = System.nanoTime();
        return new SearchResult("boyer-moore", milliseconds(preparationStart, searchStart),
                milliseconds(searchStart, searchEnd), papers);
    }

    private SearchResult searchWithRabinKarp(String pattern) {
        long preparationStart = System.nanoTime();
        RabinKarp.Prepared prepared = RabinKarp.prepare(pattern);
        long searchStart = System.nanoTime();
        List<PaperSearchResult> papers = new ArrayList<>();
        for (Paper paper : corpus.papers()) {
            List<Integer> positions = RabinKarp.search(paper.originalAbstract(), pattern, prepared);
            papers.add(new PaperSearchResult(paper.id(), positions));
        }
        long searchEnd = System.nanoTime();
        return new SearchResult("rabin-karp", milliseconds(preparationStart, searchStart),
                milliseconds(searchStart, searchEnd), papers);
    }

    private double milliseconds(long start, long end) {
        return (end - start) / 1_000_000.0;
    }

    public record PaperSearchResult(String paperId, List<Integer> positions) {
    }

    public record SearchResult(String method, double preprocessingMilliseconds, double searchMilliseconds,
                               List<PaperSearchResult> papers) {
    }

    public record SearchResponse(String pattern, List<SearchResult> results) {
    }
}
