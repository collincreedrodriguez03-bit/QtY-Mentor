package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.TradeReason
import com.example.model.TradeType
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperTradeDialog(
    currentPrice: Float,
    onDismiss: () -> Unit,
    onSubmit: (TradeType, Float, Float, Float, TradeReason, String) -> Unit
) {
    var isLong by remember { mutableStateOf(true) }
    var sizeStr by remember { mutableStateOf("100") }
    var slStr by remember { mutableStateOf((currentPrice * 0.98f).let { "%.1f".format(it) }) }
    var tpStr by remember { mutableStateOf((currentPrice * 1.04f).let { "%.1f".format(it) }) }
    var selectedReason by remember { mutableStateOf(TradeReason.BREAKOUT) }
    var note by remember { mutableStateOf("") }
    var expandedReason by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = QtYSurface,
        title = { Text("Paper Trade Execution", color = TextPrimary) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { isLong = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isLong) TradingGreen else QtYSurfaceVariant),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Long")
                    }
                    Button(
                        onClick = { isLong = false },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (!isLong) TradingRed else QtYSurfaceVariant),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Short")
                    }
                }

                OutlinedTextField(
                    value = sizeStr,
                    onValueChange = { sizeStr = it },
                    label = { Text("Position Size ($)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = QtYBlue,
                        unfocusedBorderColor = QtYOutline,
                        focusedLabelColor = QtYBlue,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = slStr,
                    onValueChange = { slStr = it },
                    label = { Text("Stop Loss ($)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TradingRed,
                        unfocusedBorderColor = QtYOutline,
                        focusedLabelColor = TradingRed,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tpStr,
                    onValueChange = { tpStr = it },
                    label = { Text("Take Profit ($)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TradingGreen,
                        unfocusedBorderColor = QtYOutline,
                        focusedLabelColor = TradingGreen,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expandedReason,
                    onExpandedChange = { expandedReason = it }
                ) {
                    OutlinedTextField(
                        value = selectedReason.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Trade Setup Reason") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = QtYBlue,
                            unfocusedBorderColor = QtYOutline,
                            focusedLabelColor = QtYBlue,
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedReason,
                        onDismissRequest = { expandedReason = false }
                    ) {
                        TradeReason.values().forEach { reason ->
                            DropdownMenuItem(
                                text = { Text(reason.name) },
                                onClick = {
                                    selectedReason = reason
                                    expandedReason = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Journal Note") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = QtYBlue,
                        unfocusedBorderColor = QtYOutline,
                        focusedLabelColor = QtYBlue,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val size = sizeStr.toFloatOrNull() ?: 100f
                    val sl = slStr.toFloatOrNull() ?: (currentPrice * 0.98f)
                    val tp = tpStr.toFloatOrNull() ?: (currentPrice * 1.04f)
                    onSubmit(if (isLong) TradeType.LONG else TradeType.SHORT, size, sl, tp, selectedReason, note)
                    onDismiss()
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isLong) TradingGreen else TradingRed)
            ) {
                Text("Place Trade")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}
