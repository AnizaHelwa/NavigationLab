package com.example.navigationlab.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.navigationlab.ui.detail.DetailUiState

@Composable
fun DetailScreen(
    uiState: DetailUiState,
    onBack: () -> Unit,
    onOpenProfile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Detail Student", style = MaterialTheme.typography.headlineMedium)

        when {
            uiState.isLoading -> {
                CircularProgressIndicator()
            }

            uiState.errorMessage != null -> {
                Text(
                    text = uiState.errorMessage,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error
                )
            }

            else -> {
                uiState.student?.let { data ->
                    Text(data.name, style = MaterialTheme.typography.headlineSmall)
                    Text("ID: ${data.id}")
                    Text("Email: ${data.email}")
                    Text("University: ${data.university}")
                }
            }
        }

        OutlinedButton(onClick = onOpenProfile) {
            Text("Buka Profile")
        }
        Button(onClick = onBack) {
            Text("Kembali")
        }
    }
}