package com.example.navigationlab.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.navigationlab.ui.home.HomeUiState

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onOpenDetail: (Int) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenAbout: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Daftar Mahasiswa",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(
                        12.dp,
                        Alignment.CenterVertically
                    )
                ) {
                    Text(
                        text = uiState.errorMessage,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = onRetry) {
                        Text("Coba Lagi")
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(
                        10.dp,
                        Alignment.CenterVertically
                    )
                ) {
                    items(uiState.students, key = { it.id }) { student ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenDetail(student.id) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = student.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "ID: ${student.id}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(
                12.dp,
                Alignment.CenterHorizontally
            )
        ) {
            OutlinedButton(onClick = onOpenProfile) {
                Text("Buka Profile")
            }
            OutlinedButton(onClick = onOpenAbout) {
                Text("Buka About")
            }
        }
    }
}