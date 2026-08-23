package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Exclusion
import com.example.model.ExclusionType
import com.example.ui.theme.EmeraldSafe
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExclusionsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val exclusions by viewModel.exclusions.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Exclusions & Protected List",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.DASHBOARD) },
                        modifier = Modifier.testTag("exclusions_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_exclusion_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Exclusion", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { innerPadding ->
        if (exclusions.isEmpty()) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = EmeraldSafe,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Exclusions Configured",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Add paths, extensions, or keywords to protect them from scans & cleanup.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp)
            ) {
                items(exclusions, key = { it.id }) { exclusion ->
                    ExclusionItemCard(
                        exclusion = exclusion,
                        onToggle = { enabled -> viewModel.toggleExclusion(exclusion.id, enabled) },
                        onDelete = { viewModel.removeExclusion(exclusion.id) }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddExclusionDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { pattern, type, desc ->
                    viewModel.addExclusion(pattern, type, desc)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun ExclusionItemCard(
    exclusion: Exclusion,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("exclusion_card_${exclusion.id}"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exclusion.pattern,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Type: ${exclusion.type.name} • ${if (exclusion.description.isNotBlank()) exclusion.description else "Protected rule"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = exclusion.enabled,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag("switch_exclusion_${exclusion.id}")
            )

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_exclusion_${exclusion.id}")
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

@Composable
fun AddExclusionDialog(
    onDismiss: () -> Unit,
    onAdd: (pattern: String, type: ExclusionType, desc: String) -> Unit
) {
    var pattern by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ExclusionType.FOLDER_NAME) }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add Protected Exclusion", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text("Pattern / Name") },
                    placeholder = { Text("e.g. DCIM, .pdf, /storage/...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_exclusion_pattern"),
                    singleLine = true
                )

                Text(text = "Exclusion Rule Type:", style = MaterialTheme.typography.labelMedium)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedType == ExclusionType.FOLDER_NAME,
                        onClick = { selectedType = ExclusionType.FOLDER_NAME }
                    )
                    Text(text = "Folder Name")

                    Spacer(modifier = Modifier.width(16.dp))

                    RadioButton(
                        selected = selectedType == ExclusionType.EXTENSION,
                        onClick = { selectedType = ExclusionType.EXTENSION }
                    )
                    Text(text = "Extension")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedType == ExclusionType.PATH,
                        onClick = { selectedType = ExclusionType.PATH }
                    )
                    Text(text = "Exact Path")

                    Spacer(modifier = Modifier.width(16.dp))

                    RadioButton(
                        selected = selectedType == ExclusionType.KEYWORD,
                        onClick = { selectedType = ExclusionType.KEYWORD }
                    )
                    Text(text = "Keyword")
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_exclusion_desc"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pattern.isNotBlank()) {
                        onAdd(pattern, selectedType, description)
                    }
                },
                modifier = Modifier.testTag("submit_add_exclusion_button")
            ) {
                Text(text = "Add Rule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}

