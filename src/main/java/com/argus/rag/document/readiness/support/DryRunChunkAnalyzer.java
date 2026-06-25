package com.argus.rag.document.readiness.support;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DryRunChunkAnalyzer {
    private static final int SHORT_CHUNK_THRESHOLD = 120;
    private static final int LONG_CHUNK_THRESHOLD = 2200;

    public Metrics analyze(List<Document> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return new Metrics(0, 0, 0, 0, 0, 0);
        }
        int totalLength = 0;
        int shortCount = 0;
        int longCount = 0;
        int emptyCount = 0;
        int duplicateCount = 0;
        Set<String> seen = new HashSet<>();
        for (Document chunk : chunks) {
            String text = chunk == null || chunk.getText() == null ? "" : chunk.getText().trim();
            int length = text.length();
            totalLength += length;
            if (length == 0) {
                emptyCount++;
            }
            if (length > 0 && length < SHORT_CHUNK_THRESHOLD) {
                shortCount++;
            }
            if (length > LONG_CHUNK_THRESHOLD) {
                longCount++;
            }
            String signature = normalizeSignature(text);
            if (!signature.isBlank() && !seen.add(signature)) {
                duplicateCount++;
            }
        }
        return new Metrics(
                chunks.size(),
                Math.round((float) totalLength / chunks.size()),
                shortCount,
                longCount,
                emptyCount,
                duplicateCount
        );
    }

    private String normalizeSignature(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    public record Metrics(
            int chunkCount,
            int avgChunkLength,
            int shortChunkCount,
            int longChunkCount,
            int emptyChunkCount,
            int duplicateChunkCount
    ) {
    }
}
