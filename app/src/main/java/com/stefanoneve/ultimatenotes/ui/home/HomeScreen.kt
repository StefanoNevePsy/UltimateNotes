package com.stefanoneve.ultimatenotes.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stefanoneve.ultimatenotes.data.db.FolderEntity
import com.stefanoneve.ultimatenotes.data.db.NoteEntity
import com.stefanoneve.ultimatenotes.ui.components.folderIcon
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenNote: (String) -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val folders by viewModel.folders.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val query by viewModel.query.collectAsState()
    val selectedFolderId by viewModel.selectedFolderId.collectAsState()
    val message by viewModel.message.collectAsState()

    var folderBeingEdited by remember { mutableStateOf<FolderEntity?>(null) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.message.value = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Ultimate Notes", fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Impostazioni")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.createNote(onOpenNote) }) {
                Icon(Icons.Outlined.Add, contentDescription = "Nuova nota")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.query.value = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cerca nelle note…") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
            )
            Spacer(Modifier.height(12.dp))
            FolderRow(
                folders = folders,
                selectedFolderId = selectedFolderId,
                onSelect = { viewModel.selectedFolderId.value = it },
                onAdd = {
                    folderBeingEdited = null
                    showFolderDialog = true
                },
                onEdit = {
                    folderBeingEdited = it
                    showFolderDialog = true
                },
            )
            Spacer(Modifier.height(12.dp))
            if (notes.isEmpty()) {
                EmptyState()
            } else {
                NotesGrid(
                    notes = notes,
                    folders = folders,
                    onOpen = onOpenNote,
                    onTogglePin = viewModel::togglePin,
                    onDelete = { viewModel.deleteNote(it.id) },
                    onMove = viewModel::moveNote,
                )
            }
        }
    }

    if (showFolderDialog) {
        FolderDialog(
            initial = folderBeingEdited,
            onDismiss = { showFolderDialog = false },
            onSave = {
                viewModel.saveFolder(it)
                showFolderDialog = false
            },
            onDelete = folderBeingEdited?.let { folder ->
                {
                    viewModel.deleteFolder(folder.id)
                    showFolderDialog = false
                }
            },
        )
    }

    if (showSettings) {
        val exportLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/zip"),
        ) { uri -> uri?.let(viewModel::exportBackup) }
        val importLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri -> uri?.let(viewModel::importBackup) }
        val fontLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri -> uri?.let(viewModel::importFont) }

        SettingsSheet(
            settingsStore = viewModel.settingsStore,
            fontManager = viewModel.fontManager,
            onExportBackup = {
                exportLauncher.launch("UltimateNotes-backup-${System.currentTimeMillis()}.zip")
            },
            onImportBackup = { importLauncher.launch(arrayOf("application/zip")) },
            onImportFont = {
                fontLauncher.launch(arrayOf("font/ttf", "font/otf", "application/octet-stream"))
            },
            onDismiss = { showSettings = false },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderRow(
    folders: List<FolderEntity>,
    selectedFolderId: String?,
    onSelect: (String?) -> Unit,
    onAdd: () -> Unit,
    onEdit: (FolderEntity) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FolderChip(
                label = "Tutte",
                color = MaterialTheme.colorScheme.primary,
                icon = Icons.Outlined.StickyNote2,
                selected = selectedFolderId == null,
                onClick = { onSelect(null) },
            )
        }
        items(folders, key = { it.id }) { folder ->
            FolderChip(
                label = folder.name,
                color = Color(folder.color),
                icon = folderIcon(folder.icon),
                selected = selectedFolderId == folder.id,
                onClick = { onSelect(folder.id) },
                onLongClick = { onEdit(folder) },
            )
        }
        item {
            FolderChip(
                label = "Nuova",
                color = MaterialTheme.colorScheme.outline,
                icon = Icons.Outlined.Add,
                selected = false,
                onClick = onAdd,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderChip(
    label: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val container =
        if (selected) color else MaterialTheme.colorScheme.surfaceVariant
    val content =
        if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(container)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) content else color,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(label, color = content, style = MaterialTheme.typography.labelLarge)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotesGrid(
    notes: List<NoteEntity>,
    folders: List<FolderEntity>,
    onOpen: (String) -> Unit,
    onTogglePin: (NoteEntity) -> Unit,
    onDelete: (NoteEntity) -> Unit,
    onMove: (NoteEntity, String?) -> Unit,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(170.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalItemSpacing = 10.dp,
        modifier = Modifier.fillMaxSize(),
    ) {
        items(notes, key = { it.id }) { note ->
            var menuOpen by remember { mutableStateOf(false) }
            val folder = folders.firstOrNull { it.id == note.folderId }
            Box {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onOpen(note.id) },
                            onLongClick = { menuOpen = true },
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (folder != null) {
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(folder.color)),
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                text = note.title.ifBlank { "Senza titolo" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (note.pinned) {
                                Icon(
                                    Icons.Outlined.PushPin,
                                    contentDescription = "Fissata",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        if (note.plainText.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = note.plainText,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = DateFormat.getDateInstance(DateFormat.SHORT)
                                .format(Date(note.updatedAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(if (note.pinned) "Sblocca" else "Fissa in alto") },
                        leadingIcon = { Icon(Icons.Outlined.PushPin, null) },
                        onClick = {
                            menuOpen = false
                            onTogglePin(note)
                        },
                    )
                    folders.forEach { f ->
                        if (f.id != note.folderId) {
                            DropdownMenuItem(
                                text = { Text("Sposta in \"${f.name}\"") },
                                leadingIcon = {
                                    Icon(
                                        Icons.AutoMirrored.Outlined.DriveFileMove,
                                        null,
                                        tint = Color(f.color),
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    onMove(note, f.id)
                                },
                            )
                        }
                    }
                    if (note.folderId != null) {
                        DropdownMenuItem(
                            text = { Text("Rimuovi dalla cartella") },
                            leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                            onClick = {
                                menuOpen = false
                                onMove(note, null)
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Elimina") },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Delete,
                                null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onDelete(note)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.StickyNote2,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.outlineVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Nessuna nota qui.\nTocca + per iniziare a scrivere o disegnare.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}
