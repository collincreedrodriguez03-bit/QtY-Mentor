package com.example.repository

import com.example.data.api.BinanceClient
import com.example.data.api.BinanceTickerResponse
import com.example.model.Candle
import com.example.model.TimeFrame
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class ConnectionStatus {
    LIVE, CONNECTING, RECONNECTING, OFFLINE
}

data class MarketDataState(
    val btcPrice: Float = 0.0f,
    val priceChange24h: String = "--",
    val volume24h: String = "--",
    val status: ConnectionStatus = ConnectionStatus.CONNECTING,
    val lastUpdateTimeMs: Long = 0L,
    val isStale: Boolean = true,
    val candles: List<Candle> = emptyList()
)

class MarketDataRepository {

    private val _marketDataState = MutableStateFlow(MarketDataState())
    val marketDataState: StateFlow<MarketDataState> = _marketDataState.asStateFlow()

    private val okHttpClient = OkHttpClient.Builder()
        .readTimeout(10, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null
    private var staleCheckJob: Job? = null
    private var currentInterval: String = "1h"

    init {
        startStaleChecker()
    }

    fun startLiveDataStream(timeFrame: TimeFrame) {
        currentInterval = timeFrame.binanceInterval
        _marketDataState.value = _marketDataState.value.copy(
            status = ConnectionStatus.CONNECTING
        )

        scope.launch {
            fetchInitialRestData(timeFrame)
            connectWebSocket(currentInterval)
            startPollingFallback(timeFrame)
        }
    }

    private suspend fun fetchInitialRestData(timeFrame: TimeFrame) {
        try {
            val response = BinanceClient.service.getKlines(
                symbol = "BTCUSDT",
                interval = timeFrame.binanceInterval,
                limit = 200
            )
            val parsedCandles = parseAndValidateKlines(response)
            val ticker = try { BinanceClient.service.getTicker24hr("BTCUSDT") } catch (e: Exception) { null }

            val latestPrice = parsedCandles.lastOrNull()?.close
                ?: ticker?.lastPrice?.toFloatOrNull()
                ?: _marketDataState.value.btcPrice

            _marketDataState.value = _marketDataState.value.copy(
                btcPrice = if (latestPrice > 0f) latestPrice else _marketDataState.value.btcPrice,
                priceChange24h = ticker?.let { "${it.priceChangePercent}%" } ?: _marketDataState.value.priceChange24h,
                volume24h = ticker?.let { "${"%.1f".format(it.volume.toFloatOrNull() ?: 0f)} BTC" } ?: _marketDataState.value.volume24h,
                status = ConnectionStatus.LIVE,
                lastUpdateTimeMs = System.currentTimeMillis(),
                isStale = false,
                candles = parsedCandles
            )
        } catch (e: Exception) {
            _marketDataState.value = _marketDataState.value.copy(
                status = ConnectionStatus.OFFLINE,
                isStale = true
            )
        }
    }

    private fun connectWebSocket(interval: String) {
        webSocket?.close(1000, "Switching stream")
        val streamUrl = "wss://stream.binance.com:9443/stream?streams=btcusdt@ticker/btcusdt@kline_$interval"
        val request = Request.Builder().url(streamUrl).build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                _marketDataState.value = _marketDataState.value.copy(
                    status = ConnectionStatus.LIVE,
                    isStale = false
                )
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val root = JSONObject(text)
                    val stream = root.optString("stream")
                    val data = root.optJSONObject("data") ?: return

                    if (stream.contains("ticker")) {
                        val lastPrice = data.optString("c").toFloatOrNull() ?: return
                        val priceChangePct = data.optString("P")
                        val vol = data.optString("v").toFloatOrNull() ?: 0f

                        _marketDataState.value = _marketDataState.value.copy(
                            btcPrice = lastPrice,
                            priceChange24h = "$priceChangePct%",
                            volume24h = "${"%.1f".format(vol)} BTC",
                            status = ConnectionStatus.LIVE,
                            lastUpdateTimeMs = System.currentTimeMillis(),
                            isStale = false
                        )
                    } else if (stream.contains("kline")) {
                        val k = data.optJSONObject("k") ?: return
                        val t = k.optLong("t")
                        val o = k.optString("o").toFloatOrNull() ?: 0f
                        val h = k.optString("h").toFloatOrNull() ?: 0f
                        val l = k.optString("l").toFloatOrNull() ?: 0f
                        val c = k.optString("c").toFloatOrNull() ?: 0f
                        val v = k.optString("v").toFloatOrNull() ?: 0f

                        val rawCandle = Candle(t, o, h, l, c, v)
                        val validatedCandle = validateAndCleanCandle(rawCandle)

                        val currentList = _marketDataState.value.candles.toMutableList()
                        if (currentList.isNotEmpty() && currentList.last().timestamp == validatedCandle.timestamp) {
                            currentList[currentList.size - 1] = validatedCandle
                        } else {
                            currentList.add(validatedCandle)
                            if (currentList.size > 300) {
                                currentList.removeAt(0)
                            }
                        }

                        _marketDataState.value = _marketDataState.value.copy(
                            btcPrice = validatedCandle.close,
                            status = ConnectionStatus.LIVE,
                            lastUpdateTimeMs = System.currentTimeMillis(),
                            isStale = false,
                            candles = currentList
                        )
                    }
                } catch (e: Exception) {
                    // Ignore malformed WS frame
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                _marketDataState.value = _marketDataState.value.copy(
                    status = ConnectionStatus.RECONNECTING,
                    isStale = true
                )
                scope.launch {
                    delay(3000L)
                    connectWebSocket(currentInterval)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (code != 1000) {
                    _marketDataState.value = _marketDataState.value.copy(
                        status = ConnectionStatus.RECONNECTING,
                        isStale = true
                    )
                    scope.launch {
                        delay(3000L)
                        connectWebSocket(currentInterval)
                    }
                }
            }
        })
    }

    private fun startPollingFallback(timeFrame: TimeFrame) {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive) {
                delay(10000L)
                val isWsConnected = _marketDataState.value.status == ConnectionStatus.LIVE && !_marketDataState.value.isStale
                if (!isWsConnected) {
                    try {
                        val ticker = BinanceClient.service.getTicker24hr("BTCUSDT")
                        val klines = BinanceClient.service.getKlines(
                            symbol = "BTCUSDT",
                            interval = timeFrame.binanceInterval,
                            limit = 200
                        )
                        val parsed = parseAndValidateKlines(klines)
                        val lastPrice = ticker.lastPrice.toFloatOrNull() ?: parsed.lastOrNull()?.close ?: _marketDataState.value.btcPrice

                        _marketDataState.value = _marketDataState.value.copy(
                            btcPrice = lastPrice,
                            priceChange24h = "${ticker.priceChangePercent}%",
                            volume24h = "${"%.1f".format(ticker.volume.toFloatOrNull() ?: 0f)} BTC",
                            status = ConnectionStatus.LIVE,
                            lastUpdateTimeMs = System.currentTimeMillis(),
                            isStale = false,
                            candles = if (parsed.isNotEmpty()) parsed else _marketDataState.value.candles
                        )
                    } catch (e: Exception) {
                        _marketDataState.value = _marketDataState.value.copy(
                            status = ConnectionStatus.OFFLINE,
                            isStale = true
                        )
                    }
                }
            }
        }
    }

    private fun startStaleChecker() {
        staleCheckJob?.cancel()
        staleCheckJob = scope.launch {
            while (isActive) {
                delay(5000L)
                val lastUpdate = _marketDataState.value.lastUpdateTimeMs
                if (lastUpdate > 0 && System.currentTimeMillis() - lastUpdate > 15000L) {
                    _marketDataState.value = _marketDataState.value.copy(
                        isStale = true,
                        status = if (_marketDataState.value.status == ConnectionStatus.LIVE) ConnectionStatus.RECONNECTING else _marketDataState.value.status
                    )
                }
            }
        }
    }

    // Real Historical BTC Klines for Replay Mode - Never generated or random!
    suspend fun fetchHistoricalKlines(
        timeFrame: TimeFrame,
        limit: Int = 300,
        startTime: Long? = null,
        endTime: Long? = null
    ): List<Candle> = withContext(Dispatchers.IO) {
        try {
            val response = BinanceClient.service.getKlines(
                symbol = "BTCUSDT",
                interval = timeFrame.binanceInterval,
                limit = limit,
                startTime = startTime,
                endTime = endTime
            )
            parseAndValidateKlines(response)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseAndValidateKlines(raw: List<List<Any>>): List<Candle> {
        return raw.mapNotNull { item ->
            try {
                val timestamp = (item[0] as Number).toLong()
                val open = (item[1] as String).toFloat()
                val high = (item[2] as String).toFloat()
                val low = (item[3] as String).toFloat()
                val close = (item[4] as String).toFloat()
                val volume = (item[5] as String).toFloat()
                val rawCandle = Candle(timestamp, open, high, low, close, volume)
                validateAndCleanCandle(rawCandle)
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.timestamp }
    }

    private fun validateAndCleanCandle(c: Candle): Candle {
        val o = maxOf(0.01f, c.open)
        val cl = maxOf(0.01f, c.close)
        val h = maxOf(o, cl, c.high)
        val l = maxOf(0.01f, minOf(o, cl, c.low))
        val v = maxOf(0f, c.volume)
        return c.copy(open = o, high = h, low = l, close = cl, volume = v)
    }

    fun close() {
        webSocket?.close(1000, "Repository closed")
        scope.cancel()
    }
}
