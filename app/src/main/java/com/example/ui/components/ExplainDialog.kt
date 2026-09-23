package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

@Composable
fun ExplainDialog(
    explanation: String?,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = QtYSurface,
        title = { Text("AI Chart Telemetry Analysis", color = QtYPurple) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = QtYPurple, modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    Text(explanation ?: "No explanation available.", color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Disclaimer: Technical analysis cannot guarantee future price movement. Educational use only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = QtYPurple)
            ) {
                Text("Got It")
            }
        }
    )
}
