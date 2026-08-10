package com.syntricdb.ai;

import com.syntricdb.engine.StorageEngine;
import com.syntricdb.engine.schema.Tuple;
import com.syntricdb.engine.vector.HNSWIndex;
import com.syntricdb.engine.fulltext.InvertedIndex;

import java.util.*;

public class RAGEngine {
    private final StorageEngine storageEngine;
    private final AIEngine aiEngine;

    public static class RAGResult {
        private final String prompt;
        private final List<Map<String, Object>> retrievedContext;
        private final String augmentedPrompt;
        private final String generatedAnswer;
        private final double retrievalTimeMs;

        public RAGResult(String prompt, List<Map<String, Object>> retrievedContext, String augmentedPrompt, String generatedAnswer, double retrievalTimeMs) {
            this.prompt = prompt;
            this.retrievedContext = retrievedContext;
            this.augmentedPrompt = augmentedPrompt;
            this.generatedAnswer = generatedAnswer;
            this.retrievalTimeMs = retrievalTimeMs;
        }

        public String getPrompt() { return prompt; }
        public List<Map<String, Object>> getRetrievedContext() { return retrievedContext; }
        public String getAugmentedPrompt() { return augmentedPrompt; }
        public String getGeneratedAnswer() { return generatedAnswer; }
        public double getRetrievalTimeMs() { return retrievalTimeMs; }
    }

    public RAGEngine(StorageEngine storageEngine, AIEngine aiEngine) {
        this.storageEngine = storageEngine;
        this.aiEngine = aiEngine;
    }

    public RAGResult query(String tableName, String vectorColumn, String prompt, int topK) throws Exception {
        return hybridQuery(tableName, vectorColumn, null, prompt, topK);
    }

    public RAGResult hybridQuery(String tableName, String vectorColumn, String textColumn, String prompt, int topK) throws Exception {
        long start = System.nanoTime();
        HNSWIndex hnsw = storageEngine.getVectorIndex(tableName, vectorColumn);
        InvertedIndex invertedIndex = storageEngine.getInvertedIndex(tableName);

        Map<String, Double> rrfScores = new HashMap<>();
        int kConstant = 60; // Standard RRF smoothing constant

        // 1. Vector Search RRF Scoring
        if (hnsw != null) {
            float[] queryVec = aiEngine.aiEmbed(prompt, hnsw.getDimension());
            List<HNSWIndex.VectorSearchResult> searchResults = hnsw.search(queryVec, topK * 2);

            for (int rank = 0; rank < searchResults.size(); rank++) {
                String id = searchResults.get(rank).getId();
                double score = 1.0 / (kConstant + (rank + 1));
                rrfScores.put(id, rrfScores.getOrDefault(id, 0.0) + score);
            }
        }

        // 2. Full-Text Search RRF Scoring
        if (invertedIndex != null) {
            List<InvertedIndex.SearchResult> textResults = invertedIndex.search(prompt, topK * 2);
            for (int rank = 0; rank < textResults.size(); rank++) {
                String id = textResults.get(rank).getDocId();
                double score = 1.0 / (kConstant + (rank + 1));
                rrfScores.put(id, rrfScores.getOrDefault(id, 0.0) + score);
            }
        }

        // 3. Sort by RRF combined score
        List<Map.Entry<String, Double>> sortedDocs = new ArrayList<>(rrfScores.entrySet());
        sortedDocs.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<Map<String, Object>> retrieved = new ArrayList<>();
        StringBuilder contextText = new StringBuilder();

        for (int i = 0; i < Math.min(topK, sortedDocs.size()); i++) {
            String id = sortedDocs.get(i).getKey();
            double score = sortedDocs.get(i).getValue();
            Tuple tuple = storageEngine.getByPrimaryKey(tableName, id);

            if (tuple != null) {
                Map<String, Object> item = new LinkedHashMap<>(tuple.asMap());
                item.put("_rrfScore", score);
                retrieved.add(item);

                if (tuple.get("bio") != null) {
                    contextText.append("- ").append(tuple.getString("name"))
                               .append(" (").append(tuple.getString("role")).append("): ")
                               .append(tuple.getString("bio")).append("\n");
                }
            }
        }

        String augmentedPrompt = "Context retrieved from SyntricDB:\n" + contextText + "\nUser Question: " + prompt;
        String generatedAnswer = "Based on SyntricDB hybrid vector context: Found " + retrieved.size() + " matches. Top match: " + (retrieved.isEmpty() ? "None" : retrieved.get(0).get("name"));

        long elapsed = System.nanoTime() - start;
        return new RAGResult(prompt, retrieved, augmentedPrompt, generatedAnswer, elapsed / 1_000_000.0);
    }
}
