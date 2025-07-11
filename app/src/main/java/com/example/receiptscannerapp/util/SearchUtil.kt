package com.example.receiptscannerapp.util

import kotlin.math.sqrt

object SearchUtil {
    /**
     * Computes cosine similarity between two float vectors.
     * @param vec1 First embedding vector
     * @param vec2 Second embedding vector
     * @return Cosine similarity score between 0.0 and 1.0 (or negative if vectors are opposite)
     */
    fun cosineSimilarity(vec1: List<Float>, vec2: List<Float>): Double {
        require(vec1.size == vec2.size) { "Vector size mismatch: ${vec1.size} != ${vec2.size}" }

        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        for (i in vec1.indices) {
            val a = vec1[i]
            val b = vec2[i]
            dotProduct += a * b
            normA += a * a
            normB += b * b
        }

        val denominator = sqrt((normA * normB).toDouble())
        return if (denominator != 0.0) dotProduct / denominator else 0.0
    }
}
