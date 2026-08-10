package com.syntricdb.engine.vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class HNSWIndex {
    private final int dimension;
    private final DistanceMetric metric;
    private final int m; // Max edges per node
    private final int efConstruction; // Construction beam width
    private final double mL; // Level multiplier

    private final Map<String, float[]> vectors = new ConcurrentHashMap<>();
    private final Map<String, List<Set<String>>> graph = new ConcurrentHashMap<>(); // key -> layers -> neighbors
    private final Map<String, Integer> nodeLevels = new ConcurrentHashMap<>();
    
    private volatile String entryPoint = null;
    private volatile int maxLevel = -1;

    public static class VectorSearchResult {
        private final String id;
        private final float distance;
        private final float similarity;
        private final float[] vector;

        public VectorSearchResult(String id, float distance, float similarity, float[] vector) {
            this.id = id;
            this.distance = distance;
            this.similarity = similarity;
            this.vector = vector;
        }

        public String getId() { return id; }
        public float getDistance() { return distance; }
        public float getSimilarity() { return similarity; }
        public float[] getVector() { return vector; }
    }

    public HNSWIndex(int dimension, DistanceMetric metric) {
        this(dimension, metric, 16, 64);
    }

    public HNSWIndex(int dimension, DistanceMetric metric, int m, int efConstruction) {
        this.dimension = dimension;
        this.metric = metric;
        this.m = m;
        this.efConstruction = efConstruction;
        this.mL = 1.0 / Math.log(m);
    }

    public synchronized void insert(String id, float[] vector) {
        if (vector.length != dimension) {
            throw new IllegalArgumentException("Vector dimension mismatch. Expected: " + dimension + ", got: " + vector.length);
        }

        vectors.put(id, vector);

        int level = assignRandomLevel();
        nodeLevels.put(id, level);

        List<Set<String>> layers = new ArrayList<>();
        for (int i = 0; i <= level; i++) {
            layers.add(ConcurrentHashMap.newKeySet());
        }
        graph.put(id, layers);

        if (entryPoint == null) {
            entryPoint = id;
            maxLevel = level;
            return;
        }

        String currObj = entryPoint;
        float currDist = DistanceMetric.compute(metric, vector, vectors.get(currObj));

        // Top levels greedy search
        for (int l = maxLevel; l > level; l--) {
            boolean changed = true;
            while (changed) {
                changed = false;
                Set<String> neighbors = getNeighbors(currObj, l);
                for (String neighbor : neighbors) {
                    float d = DistanceMetric.compute(metric, vector, vectors.get(neighbor));
                    if (d < currDist) {
                        currDist = d;
                        currObj = neighbor;
                        changed = true;
                    }
                }
            }
        }

        // Connect in lower levels
        for (int l = Math.min(level, maxLevel); l >= 0; l--) {
            PriorityQueue<Candidate> candidates = searchLayer(vector, currObj, efConstruction, l);
            List<String> neighbors = selectNeighbors(candidates, m);

            for (String neighbor : neighbors) {
                connectNodes(id, neighbor, l);
            }
        }

        if (level > maxLevel) {
            maxLevel = level;
            entryPoint = id;
        }
    }

    public List<VectorSearchResult> search(float[] queryVector, int k) {
        if (entryPoint == null || vectors.isEmpty()) {
            return Collections.emptyList();
        }

        String currObj = entryPoint;
        float currDist = DistanceMetric.compute(metric, queryVector, vectors.get(currObj));

        // Top levels navigation
        for (int l = maxLevel; l > 0; l--) {
            boolean changed = true;
            while (changed) {
                changed = false;
                Set<String> neighbors = getNeighbors(currObj, l);
                for (String neighbor : neighbors) {
                    float d = DistanceMetric.compute(metric, queryVector, vectors.get(neighbor));
                    if (d < currDist) {
                        currDist = d;
                        currObj = neighbor;
                        changed = true;
                    }
                }
            }
        }

        // Bottom level search with beam width
        int ef = Math.max(k, 32);
        PriorityQueue<Candidate> candidates = searchLayer(queryVector, currObj, ef, 0);

        List<VectorSearchResult> results = new ArrayList<>();
        PriorityQueue<Candidate> topK = new PriorityQueue<>(Comparator.comparingDouble((Candidate c) -> c.distance).reversed());

        while (!candidates.isEmpty()) {
            Candidate c = candidates.poll();
            topK.add(c);
            if (topK.size() > k) topK.poll();
        }

        List<Candidate> sorted = new ArrayList<>(topK);
        sorted.sort(Comparator.comparingDouble(c -> c.distance));

        for (Candidate c : sorted) {
            float dist = c.distance;
            float sim = metric == DistanceMetric.COSINE ? DistanceMetric.cosineSimilarity(queryVector, vectors.get(c.id)) : 1.0f / (1.0f + dist);
            results.add(new VectorSearchResult(c.id, dist, sim, vectors.get(c.id)));
        }

        return results;
    }

    private PriorityQueue<Candidate> searchLayer(float[] queryVector, String entryPointId, int ef, int level) {
        Set<String> visited = new HashSet<>();
        PriorityQueue<Candidate> vCandidates = new PriorityQueue<>(Comparator.comparingDouble(c -> c.distance));
        PriorityQueue<Candidate> wResult = new PriorityQueue<>(Comparator.comparingDouble((Candidate c) -> c.distance).reversed());

        float d = DistanceMetric.compute(metric, queryVector, vectors.get(entryPointId));
        Candidate epCandidate = new Candidate(entryPointId, d);

        visited.add(entryPointId);
        vCandidates.add(epCandidate);
        wResult.add(epCandidate);

        while (!vCandidates.isEmpty()) {
            Candidate curr = vCandidates.poll();
            Candidate furthest = wResult.peek();

            if (curr.distance > furthest.distance) break;

            Set<String> neighbors = getNeighbors(curr.id, level);
            for (String neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    float dist = DistanceMetric.compute(metric, queryVector, vectors.get(neighbor));
                    Candidate nCandidate = new Candidate(neighbor, dist);

                    if (dist < furthest.distance || wResult.size() < ef) {
                        vCandidates.add(nCandidate);
                        wResult.add(nCandidate);
                        if (wResult.size() > ef) {
                            wResult.poll();
                        }
                    }
                }
            }
        }

        return wResult;
    }

    private List<String> selectNeighbors(PriorityQueue<Candidate> candidates, int maxNeighbors) {
        List<Candidate> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator.comparingDouble(c -> c.distance));

        List<String> result = new ArrayList<>();
        for (int i = 0; i < Math.min(maxNeighbors, sorted.size()); i++) {
            result.add(sorted.get(i).id);
        }
        return result;
    }

    private void connectNodes(String u, String v, int level) {
        Set<String> uNeighbors = getNeighbors(u, level);
        Set<String> vNeighbors = getNeighbors(v, level);
        uNeighbors.add(v);
        vNeighbors.add(u);
    }

    private Set<String> getNeighbors(String id, int level) {
        List<Set<String>> layers = graph.get(id);
        if (layers != null && level < layers.size()) {
            return layers.get(level);
        }
        return Collections.emptySet();
    }

    private int assignRandomLevel() {
        double r = ThreadLocalRandom.current().nextDouble();
        if (r == 0) r = 0.0000001;
        return (int) (-Math.log(r) * mL);
    }

    public int size() { return vectors.size(); }
    public int getDimension() { return dimension; }
    public Map<String, float[]> getAllVectors() { return Collections.unmodifiableMap(vectors); }

    public synchronized void remove(String id) {
        vectors.remove(id);
        nodeLevels.remove(id);
        List<Set<String>> layers = graph.remove(id);
        if (layers != null) {
            for (Set<String> neighborSet : layers) {
                for (String neighborId : neighborSet) {
                    List<Set<String>> nLayers = graph.get(neighborId);
                    if (nLayers != null) {
                        for (Set<String> s : nLayers) {
                            s.remove(id);
                        }
                    }
                }
            }
        }
        if (id.equals(entryPoint)) {
            entryPoint = vectors.isEmpty() ? null : vectors.keySet().iterator().next();
        }
    }

    public Map<String, float[]> getVectors() { return Collections.unmodifiableMap(vectors); }
    public DistanceMetric getMetric() { return metric; }


    public synchronized void saveToFile(java.nio.file.Path file) throws java.io.IOException {
        if (file.getParent() != null) {
            java.nio.file.Files.createDirectories(file.getParent());
        }
        Map<String, Object> data = new HashMap<>();
        data.put("dimension", dimension);
        data.put("metric", metric.name());
        Map<String, List<Float>> vecMap = new HashMap<>();
        for (Map.Entry<String, float[]> e : vectors.entrySet()) {
            List<Float> list = new ArrayList<>(e.getValue().length);
            for (float f : e.getValue()) list.add(f);
            vecMap.put(e.getKey(), list);
        }
        data.put("vectors", vecMap);
        new com.fasterxml.jackson.databind.ObjectMapper().writeValue(file.toFile(), data);
    }

    @SuppressWarnings("unchecked")
    public synchronized void loadFromFile(java.nio.file.Path file) throws java.io.IOException {
        if (!java.nio.file.Files.exists(file)) return;
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        Map<String, Object> data = mapper.readValue(file.toFile(), Map.class);
        Map<String, List<Double>> vecMap = (Map<String, List<Double>>) data.get("vectors");
        if (vecMap != null) {
            for (Map.Entry<String, List<Double>> e : vecMap.entrySet()) {
                List<Double> list = e.getValue();
                float[] arr = new float[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    arr[i] = list.get(i).floatValue();
                }
                insert(e.getKey(), arr);
            }
        }
    }

    private static class Candidate {
        final String id;
        final float distance;

        Candidate(String id, float distance) {
            this.id = id;
            this.distance = distance;
        }
    }
}

