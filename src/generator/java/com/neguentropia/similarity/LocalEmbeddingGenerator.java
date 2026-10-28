package com.neguentropia.similarity;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import com.neguentropia.corpus.CorpusLoader;
import com.neguentropia.corpus.TextPreparation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

public final class LocalEmbeddingGenerator {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String MODEL_NAME = "intfloat/multilingual-e5-small";
    private static final int TOKEN_LIMIT = 512;

    private LocalEmbeddingGenerator() {
    }

    public static void main(String[] args) {
        if (args.length != 4) {
            System.err.println("Usage: LocalEmbeddingGenerator <corpus.json> <model.onnx> <tokenizer.json> <output.json>");
            System.exit(1);
        }
        try {
            generate(Path.of(args[0]), args[1], Path.of(args[2]), Path.of(args[3]));
        } catch (IllegalArgumentException failure) {
            System.err.println(failure.getMessage());
            System.exit(1);
        } catch (Exception failure) {
            System.err.println("Local embedding generation failed.");
            System.exit(1);
        }
    }

    private static void generate(Path manifest, String model, Path tokenizerFile, Path output) throws Exception {
        List<CorpusLoader.Paper> papers = new CorpusLoader(JSON).load(manifest);
        ArrayList<Map<String, Object>> vectors = new ArrayList<>();
        Map<String, String> options = Map.of("truncation", "false", "padding", "false");
        OrtEnvironment environment = OrtEnvironment.getEnvironment();
        try (HuggingFaceTokenizer tokenizer = HuggingFaceTokenizer.newInstance(tokenizerFile, options);
             OrtSession session = environment.createSession(model, new OrtSession.SessionOptions())) {
            for (CorpusLoader.Paper paper : papers) {
                Encoding encoding = tokenizer.encode(formattedInput(paper.originalAbstract()));
                long[] ids = encoding.getIds();
                requireWithinLimit(ids.length);
                long[] mask = encoding.getAttentionMask();
                double[][] states = run(environment, session, ids, mask);
                double[] values = normalize(meanPool(states, mask));
                vectors.add(Map.of("id", paper.id(), "values", values));
            }
        }
        Files.writeString(output, JSON.writeValueAsString(Map.of("model", MODEL_NAME, "papers", vectors)));
    }

    private static double[][] run(OrtEnvironment environment, OrtSession session, long[] ids, long[] mask)
            throws Exception {
        HashMap<String, OnnxTensor> inputs = new HashMap<>();
        try {
            inputs.put("input_ids", OnnxTensor.createTensor(environment, new long[][]{ids}));
            inputs.put("attention_mask", OnnxTensor.createTensor(environment, new long[][]{mask}));
            inputs.put("token_type_ids", OnnxTensor.createTensor(environment, new long[][]{new long[ids.length]}));
            try (OrtSession.Result result = session.run(inputs)) {
                float[][][] output = (float[][][]) result.get(0).getValue();
                double[][] states = new double[ids.length][];
                for (int i = 0; i < ids.length; i++) {
                    states[i] = new double[output[0][i].length];
                    for (int j = 0; j < states[i].length; j++) {
                        states[i][j] = output[0][i][j];
                    }
                }
                return states;
            }
        } finally {
            for (OnnxTensor tensor : inputs.values()) {
                tensor.close();
            }
        }
    }

    public static String formattedInput(String originalAbstract) {
        return "query: " + TextPreparation.baseText(originalAbstract);
    }

    public static int requireWithinLimit(int tokenCount) {
        if (tokenCount > TOKEN_LIMIT) {
            throw new IllegalArgumentException("Input has " + tokenCount + " tokens; the limit is 512.");
        }
        return tokenCount;
    }

    public static double[] meanPool(double[][] states, long[] mask) {
        double[] total = new double[states[0].length];
        int counted = 0;
        for (int i = 0; i < states.length; i++) {
            if (mask[i] == 1) {
                counted++;
                for (int j = 0; j < total.length; j++) {
                    total[j] += states[i][j];
                }
            }
        }
        for (int j = 0; j < total.length; j++) {
            total[j] = total[j] / counted;
        }
        return total;
    }

    public static double[] normalize(double[] values) {
        double squared = 0;
        for (double value : values) {
            squared += value * value;
        }
        double norm = Math.sqrt(squared);
        double[] normalized = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            normalized[i] = values[i] / norm;
        }
        return normalized;
    }
}
