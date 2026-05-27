package com.raju.edutrack

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private const val BATCHES_FILE_NAME = "batches.json"

object BatchStorage {

    fun loadBatches(context: Context): List<Batch> {
        val file = File(context.filesDir, BATCHES_FILE_NAME)
        if (!file.exists()) {
            return emptyList()
        }

        val raw = file.readText()
        if (raw.isBlank()) {
            return emptyList()
        }

        val batches = mutableListOf<Batch>()
        val array = JSONArray(raw)
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val name = item.optString("name")
            if (name.isNotBlank()) {
                batches.add(
                    Batch(
                        name = name,
                        timeText = item.optString("timeText"),
                        messageTemplate = item.optString("messageTemplate")
                    )
                )
            }
        }

        return batches
    }

    fun saveBatches(
        context: Context,
        batches: List<Batch>
    ) {
        val array = JSONArray()
        batches.forEach { batch ->
            val item = JSONObject()
            item.put("name", batch.name)
            item.put("timeText", batch.timeText)
            item.put("messageTemplate", batch.messageTemplate)
            array.put(item)
        }

        val file = File(context.filesDir, BATCHES_FILE_NAME)
        file.writeText(array.toString())
    }
}
