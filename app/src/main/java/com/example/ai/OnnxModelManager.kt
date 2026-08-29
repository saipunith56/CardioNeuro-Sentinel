package com.example.ai

import android.content.Context
import android.util.Log
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe manager for loading and caching ONNX model sessions from assets.
 */
object OnnxModelManager {
    private const val TAG = "OnnxModelManager"
    
    val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val sessionCache = ConcurrentHashMap<String, OrtSession>()

    /**
     * Gets/loads an [OrtSession] for the given [modelAssetPath].
     * Throws [IOException] if loading fails.
     */
    @Throws(IOException::class)
    fun getSession(context: Context, modelAssetPath: String): OrtSession {
        return sessionCache.computeIfAbsent(modelAssetPath) { path ->
            Log.d(TAG, "Loading ONNX model session for: $path")
            try {
                context.assets.open(path).use { inputStream ->
                    val modelBytes = inputStream.readBytes()
                    env.createSession(modelBytes)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading ONNX model $path: ${e.message}", e)
                throw RuntimeException("Failed to load model file: $path", e)
            }
        }
    }

    /**
     * Closes all cached sessions. Should be called when the app is terminating
     * or when clearing resources.
     */
    fun close() {
        Log.d(TAG, "Closing all cached ONNX sessions")
        for ((path, session) in sessionCache) {
            try {
                session.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing session for $path: ${e.message}", e)
            }
        }
        sessionCache.clear()
    }
}
