package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

data class Lesson(val title: String, val summary: String, val detail: String)

@Composable
fun LearnScreen(viewModel: MainViewModel) {
    val lessons = listOf(
        Lesson("1. Candles & Wicks", "Show open, high, low, and close prices over a specific timeframe.", "Green candles indicate closing higher than open; red candles indicate closing lower. Wicks show the extreme high and low price rejections reached during the candle's period."),
        Lesson("2. Volume & Liquidity", "Volume represents the total amount of BTC traded during a given period.", "High volume validates moves (e.g. breakouts), while low volume moves are often unreliable or false. Liquidity clusters around major swing highs and lows."),
        Lesson("3. Trends", "The general direction in which BTC price is moving over time.", "Identifying trends correctly helps align your trades with dominant market momentum rather than fighting the prevailing tide."),
        Lesson("4. Support & Resistance", "Support is a price floor where buying interest halts downward drops; resistance is a ceiling.", "These horizontal levels are critical decision zones where supply and demand shift dramatically."),
        Lesson("5. Breakouts", "When price decisively moves beyond a established support or resistance boundary.", "True breakouts feature expanding volume and strong momentum candles closing outside the level."),
        Lesson("6. False Breakouts", "A trap where price briefly pushes past a key level before rapidly reversing back inside.", "Beginner traders often get caught chasing false breakouts. Waiting for a retest helps filter these out."),
        Lesson("7. Retests", "When price returns to test a previously broken support or resistance level.", "A successful retest confirms that the broken level has flipped roles (resistance becomes support)."),
        Lesson("8. Higher Highs & Higher Lows (HH/HL)", "The foundational definition of an active bull market trend.", "Each swing high exceeds the previous peak, and each pullback holds above the previous low."),
        Lesson("9. Lower Highs & Lower Lows (LH/LL)", "The foundational definition of a bear market trend.", "Each peak is lower than the last, and each drop breaks below prior support floors."),
        Lesson("10. Momentum", "The speed and strength behind price movements.", "Measured via indicators like RSI and MACD or observed through large, consecutive impulsive candles."),
        Lesson("11. Consolidation", "A period of sideways price action where buyers and sellers are in equilibrium.", "Tight consolidation ranges often precede explosive impulsive breakouts."),
        Lesson("12. EMA (9, 20, 50, 200)", "Exponential Moving Averages give more weight to recent price action.", "Fast EMAs (9, 20) track short-term momentum, while 50 and 200 EMAs signal intermediate and long-term trend bias."),
        Lesson("13. RSI 14 (Relative Strength Index)", "A momentum oscillator measuring speed and change of price movements from 0 to 100.", "Values above 70 suggest overbought conditions; values below 30 suggest oversold conditions."),
        Lesson("14. MACD", "Moving Average Convergence Divergence tracks relationship between two moving averages.", "MACD line crossing above the signal line indicates bullish momentum; crossing below indicates bearish momentum."),
        Lesson("15. VWAP", "Volume Weighted Average Price shows the true average price traded based on volume.", "Institutions use VWAP as a key benchmark for fair value during the trading session."),
        Lesson("16. Bollinger Bands", "Volatility indicator consisting of an upper band, lower band, and moving average middle line.", "When bands squeeze (narrow), a period of high volatility expansion is typically imminent."),
        Lesson("17. Risk Management", "Protecting capital using stop-losses, position sizing, and proper risk/reward ratios.", "No trading strategy wins 100% of the time. Strict risk management ensures longevity and profitability.")
    )

    var selectedLesson by remember { mutableStateOf<Lesson?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Price Action & Telemetry Academy", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text("Master BTC market structure, indicators, and risk management.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(lessons) { lesson ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = QtYSurface),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, QtYOutline, RoundedCornerShape(10.dp)),
                    onClick = { selectedLesson = lesson }
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(lesson.title, style = MaterialTheme.typography.titleSmall, color = QtYBlue)
                        Text(lesson.summary, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
        }

        if (selectedLesson != null) {
            AlertDialog(
                onDismissRequest = { selectedLesson = null },
                containerColor = QtYSurface,
                title = { Text(selectedLesson!!.title, color = QtYBlue) },
                text = { Text(selectedLesson!!.detail, color = TextPrimary) },
                confirmButton = {
                    Button(
                        onClick = { selectedLesson = null },
                        colors = ButtonDefaults.buttonColors(containerColor = QtYBlue)
                    ) {
                        Text("Close")
                    }
                }
            )
        }
    }
}
