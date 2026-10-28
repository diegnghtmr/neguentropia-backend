package com.neguentropia.clustering;

import com.neguentropia.clustering.HierarchicalClustering.Merge;
import com.neguentropia.clustering.HierarchicalClustering.Result;
import com.neguentropia.corpus.CorpusLoader.Paper;
import com.neguentropia.corpus.CorpusService;
import com.neguentropia.corpus.TextPreparation;
import com.neguentropia.similarity.TfIdf;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ClusteringService {
    private static final List<String> LINKAGES = List.of("single-linkage", "complete-linkage", "average-linkage");
    private final CorpusService corpus;
    private final double[][] distances;

    public ClusteringService(CorpusService corpus) {
        this.corpus = corpus;
        if (corpus.ready()) {
            List<List<String>> tokens = new ArrayList<>();
            for (Paper paper : corpus.papers()) {
                tokens.add(TextPreparation.tokens(paper.originalAbstract()));
            }
            double[][] vectors = TfIdf.fit(tokens);
            distances = new double[vectors.length][vectors.length];
            for (int left = 0; left < vectors.length; left++) {
                for (int right = left + 1; right < vectors.length; right++) {
                    double distance = 1 - TfIdf.cosine(vectors[left], vectors[right]);
                    distances[left][right] = distance;
                    distances[right][left] = distance;
                }
            }
        } else {
            distances = new double[0][];
        }
    }

    public boolean ready() {
        return corpus.ready();
    }

    public ClusteringResponse cluster(int clusterCount) {
        List<TreeLeaf> leaves = new ArrayList<>();
        for (int leaf = 0; leaf < corpus.papers().size(); leaf++) {
            leaves.add(new TreeLeaf(leaf, corpus.papers().get(leaf).id()));
        }
        List<ClusteringResult> results = new ArrayList<>();
        for (String linkage : LINKAGES) {
            Result result = HierarchicalClustering.cluster(distances, linkage, clusterCount);
            List<Cluster> partition = new ArrayList<>();
            for (List<Integer> group : result.partition()) {
                List<String> paperIds = new ArrayList<>();
                for (int leaf : group) {
                    paperIds.add(corpus.papers().get(leaf).id());
                }
                partition.add(new Cluster(paperIds));
            }
            results.add(new ClusteringResult(linkage, result.merges(), partition,
                    Silhouette.score(distances, result.partition())));
        }
        return new ClusteringResponse(leaves, results);
    }

    public record TreeLeaf(int id, String paperId) {
    }

    public record Cluster(List<String> paperIds) {
    }

    public record ClusteringResult(String method, List<Merge> merges, List<Cluster> partition,
                                   double silhouette) {
    }

    public record ClusteringResponse(List<TreeLeaf> leaves, List<ClusteringResult> results) {
    }
}
