package com.example.backtestdata

import com.example.data.singleModels.StockAggregationResponse
import com.example.data.singleModels.StockBar
import kotlinx.serialization.json.*
import java.io.File

class JsonToDataClassConverter {
    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }

        fun stockAggregationResponseFromResource(filename: String): StockAggregationResponse {

            val cwd = File(System.getProperty("user.dir"), "src/test/kotlin/com/example/backtestdata")
            val file = File(cwd, filename)
            if(!file.exists() || !file.isFile()) {
                error("File does not exist: $filename")
            }
            val text = file.readText()

            val root: JsonElement = json.parseToJsonElement(text)

            if (root is JsonObject && "bars" in root) {
                return json.decodeFromJsonElement(root)
            }

            // If JSON is a raw array of bars, infer symbol from filename ("AAPL_1.json" -> "AAPL")
            if (root is JsonArray) {
                val inferredSymbol = filename
                    .substringAfterLast('/', filename)
                    .substringAfterLast('\\')
                    .substringBefore('_')
                    .substringBefore('.')
                    .ifBlank { error("Cannot infer symbol from filename: $filename") }

                val bars = json.decodeFromJsonElement<List<StockBar>>(root)
                return StockAggregationResponse(
                    bars = mapOf(inferredSymbol to bars),
                    nextPageToken = null
                )
            }

            if (root is JsonObject) {
                val obj = root.jsonObject
                val maybeToken = obj["next_page_token"]?.jsonPrimitive?.contentOrNull
                val symbolKey = obj.keys.firstOrNull { it != "next_page_token" }
                    ?: error("Unsupported JSON: object has no symbol key")

                val barsEl = obj[symbolKey]
                if (barsEl is JsonArray) {
                    val bars = json.decodeFromJsonElement<List<StockBar>>(barsEl)
                    return StockAggregationResponse(
                        bars = mapOf(symbolKey to bars),
                        nextPageToken = maybeToken
                    )
                }
            }

            error("Unsupported JSON shape. Expected Alpaca response, array of bars, or {SYMBOL:[...]}.")
        }
    }
}