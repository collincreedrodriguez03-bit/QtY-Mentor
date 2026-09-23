package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun CandlestickChart(
    candles: List<Candle>,
    drawings: List<DrawingObject>,
    activeIndicators: Set<IndicatorType>,
    isBeginnerMode: Boolean,
    modifier: Modifier = Modifier,
    onCandleSelected: (Candle?) -> Unit = {}
) {
    if (candles.isEmpty()) {
        Box(
            modifier = modifier.background(QtYSurface, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading real BTC chart data...", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    var scale by remember { mutableStateOf(1.0f) }
    var offsetX by remember { mutableStateOf(0f) }
    var selectedCandleIndex by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = modifier
            .background(QtYSurface, RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 10.0f)
                    offsetX += pan.x
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val width = size.width - 70f
                    val visibleCount = (candles.size / scale).toInt().coerceIn(10, candles.size)
                    val startIndex = (candles.size - visibleCount - (offsetX / (width / visibleCount)).toInt()).coerceIn(0, candles.size - visibleCount)
                    val candleWidth = width / visibleCount
                    val indexInVisible = ((tapOffset.x) / candleWidth).toInt()
                    val targetIndex = startIndex + indexInVisible
                    if (targetIndex in candles.indices) {
                        selectedCandleIndex = targetIndex
                        onCandleSelected(candles[targetIndex])
                    } else {
                        selectedCandleIndex = null
                        onCandleSelected(null)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val chartHeight = height * 0.72f
            val volumeHeight = height * 0.20f

            if (candles.isEmpty()) return@Canvas

            val visibleCount = (candles.size / scale).toInt().coerceIn(10, candles.size)
            val startIndex = (candles.size - visibleCount - (offsetX / ((width - 70f) / visibleCount)).toInt()).coerceIn(0, candles.size - visibleCount)
            val endIndex = (startIndex + visibleCount).coerceIn(0, candles.size)
            val visibleCandles = candles.subList(startIndex, endIndex)

            if (visibleCandles.isEmpty()) return@Canvas

            val maxPrice = visibleCandles.maxOf { it.high }
            val minPrice = visibleCandles.minOf { it.low }
            val priceRange = (maxPrice - minPrice).coerceAtLeast(1.0f)
            val maxVolume = visibleCandles.maxOf { it.volume }.coerceAtLeast(1.0f)

            fun priceToY(price: Float): Float {
                return chartHeight - ((price - minPrice) / priceRange) * (chartHeight - 30f)
            }

            // Draw grid lines & price scale
            val steps = 5
            for (i in 0..steps) {
                val price = minPrice + (priceRange / steps) * i
                val y = priceToY(price)
                drawLine(GridLineColor, Offset(0f, y), Offset(width - 70f, y), 1f)
                drawContext.canvas.nativeCanvas.drawText(
                    "$%.1f".format(price),
                    width - 65f,
                    y + 4f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#787B86")
                        textSize = 20f
                    }
                )
            }

            val candleWidth = (width - 70f) / visibleCandles.size

            // Draw Volume
            if (activeIndicators.contains(IndicatorType.VOLUME)) {
                visibleCandles.forEachIndexed { index, candle ->
                    val x = index * candleWidth + candleWidth / 2
                    val vHeight = (candle.volume / maxVolume) * volumeHeight
                    val vTop = height - vHeight
                    val color = if (candle.isGreen) TradingGreen.copy(alpha = 0.35f) else TradingRed.copy(alpha = 0.35f)
                    drawRect(
                        color = color,
                        topLeft = Offset(x - candleWidth * 0.35f, vTop),
                        size = Size(candleWidth * 0.7f, vHeight)
                    )
                }
            }

            // Draw Candlesticks
            visibleCandles.forEachIndexed { index, candle ->
                val x = index * candleWidth + candleWidth / 2
                val openY = priceToY(candle.open)
                val closeY = priceToY(candle.close)
                val highY = priceToY(candle.high)
                val lowY = priceToY(candle.low)
                val color = if (candle.isGreen) TradingGreen else TradingRed

                // Wick
                drawLine(color, Offset(x, highY), Offset(x, lowY), 1.5f)

                // Body
                val topBody = minOf(openY, closeY)
                val bottomBody = maxOf(openY, closeY)
                val bodyHeight = (bottomBody - topBody).coerceAtLeast(2f)

                drawRect(
                    color = color,
                    topLeft = Offset(x - candleWidth * 0.4f, topBody),
                    size = Size(candleWidth * 0.8f, bodyHeight)
                )
            }

            // Draw EMAs
            if (activeIndicators.contains(IndicatorType.EMA_9)) {
                drawEmaLine(candles, startIndex, endIndex, candleWidth, ::priceToY, 9, Color(0xFFFFEB3B))
            }
            if (activeIndicators.contains(IndicatorType.EMA_20)) {
                drawEmaLine(candles, startIndex, endIndex, candleWidth, ::priceToY, 20, QtYBlue)
            }
            if (activeIndicators.contains(IndicatorType.EMA_50)) {
                drawEmaLine(candles, startIndex, endIndex, candleWidth, ::priceToY, 50, Color(0xFFFF9800))
            }
            if (activeIndicators.contains(IndicatorType.EMA_200)) {
                drawEmaLine(candles, startIndex, endIndex, candleWidth, ::priceToY, 200, QtYPurple)
            }

            // Draw Bollinger Bands (20, 2)
            if (activeIndicators.contains(IndicatorType.BOLLINGER_BANDS) && candles.size >= 20) {
                val period = 20
                val upperPath = Path()
                val lowerPath = Path()
                val middlePath = Path()
                var first = true

                for (i in startIndex until endIndex) {
                    if (i < period) continue
                    val sub = candles.subList(i - period + 1, i + 1)
                    val sma = sub.sumOf { it.close.toDouble() }.toFloat() / period
                    val variance = sub.sumOf { (it.close - sma).let { d -> (d * d).toDouble() } } / period
                    val stdDev = kotlin.math.sqrt(variance).toFloat()
                    val upper = sma + (2f * stdDev)
                    val lower = sma - (2f * stdDev)

                    val x = (i - startIndex) * candleWidth + candleWidth / 2
                    val yMid = priceToY(sma)
                    val yUp = priceToY(upper)
                    val yLow = priceToY(lower)

                    if (first) {
                        middlePath.moveTo(x, yMid)
                        upperPath.moveTo(x, yUp)
                        lowerPath.moveTo(x, yLow)
                        first = false
                    } else {
                        middlePath.lineTo(x, yMid)
                        upperPath.lineTo(x, yUp)
                        lowerPath.lineTo(x, yLow)
                    }
                }
                drawPath(middlePath, Color.Gray, style = Stroke(width = 1f))
                drawPath(upperPath, Color.Cyan.copy(alpha = 0.6f), style = Stroke(width = 1.2f))
                drawPath(lowerPath, Color.Cyan.copy(alpha = 0.6f), style = Stroke(width = 1.2f))
            }

            // Draw VWAP
            if (activeIndicators.contains(IndicatorType.VWAP)) {
                val vwapPath = Path()
                var cumVol = 0.0f
                var cumTypicalVol = 0.0f
                visibleCandles.forEachIndexed { index, candle ->
                    val typicalPrice = (candle.high + candle.low + candle.close) / 3f
                    cumVol += candle.volume
                    cumTypicalVol += typicalPrice * candle.volume
                    val vwap = if (cumVol > 0) cumTypicalVol / cumVol else typicalPrice
                    val x = index * candleWidth + candleWidth / 2
                    val y = priceToY(vwap)
                    if (index == 0) vwapPath.moveTo(x, y) else vwapPath.lineTo(x, y)
                }
                drawPath(vwapPath, Color(0xFFFF4081), style = Stroke(width = 2f))
            }

            // Current Price Line
            val lastPrice = candles.last().close
            val lastY = priceToY(lastPrice)
            drawLine(QtYBlue, Offset(0f, lastY), Offset(width - 70f, lastY), 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))

            // Highlight selected candle crosshair
            if (selectedCandleIndex != null && selectedCandleIndex!! in startIndex until endIndex) {
                val relIdx = selectedCandleIndex!! - startIndex
                val selX = relIdx * candleWidth + candleWidth / 2
                drawLine(Color.White.copy(alpha = 0.6f), Offset(selX, 0f), Offset(selX, chartHeight), 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
            }
        }

        // TradingView Style Inspection Bar (When Candle Selected)
        if (selectedCandleIndex != null && selectedCandleIndex!! < candles.size) {
            val c = candles[selectedCandleIndex!!]
            val dateStr = android.text.format.DateFormat.format("yyyy-MM-dd HH:mm", c.timestamp).toString()
            val isG = c.isGreen
            val textColor = if (isG) TradingGreen else TradingRed

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp)
                    .background(QtYSurfaceVariant.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = dateStr, color = TextSecondary, fontSize = 11.sp)
                Text(
                    text = "O: $${"%.1f".format(c.open)}  H: $${"%.1f".format(c.high)}  L: $${"%.1f".format(c.low)}  C: $${"%.1f".format(c.close)}",
                    color = textColor,
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEmaLine(
    candles: List<Candle>,
    startIndex: Int,
    endIndex: Int,
    candleWidth: Float,
    priceToY: (Float) -> Float,
    period: Int,
    color: Color
) {
    val k = 2.0f / (period + 1)
    val emaPath = Path()
    var first = true
    var prevEMA: Float? = null

    for (i in candles.indices) {
        val c = candles[i]
        if (i < period - 1) {
            continue
        } else if (i == period - 1) {
            val sma = candles.subList(0, period).sumOf { it.close.toDouble() }.toFloat() / period
            prevEMA = sma
        } else {
            prevEMA = (c.close * k) + (prevEMA!! * (1 - k))
        }

        if (i in startIndex until endIndex && prevEMA != null) {
            val x = (i - startIndex) * candleWidth + candleWidth / 2
            val y = priceToY(prevEMA!!)
            if (first) {
                emaPath.moveTo(x, y)
                first = false
            } else {
                emaPath.lineTo(x, y)
            }
        }
    }
    drawPath(emaPath, color, style = Stroke(width = 1.8f))
}
