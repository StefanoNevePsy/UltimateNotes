package com.stefanoneve.ultimatenotes.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.FolderInput
import com.composables.icons.lucide.FolderPlus
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.Pin
import com.composables.icons.lucide.PinOff
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Settings2
import com.composables.icons.lucide.StickyNote
import com.composables.icons.lucide.Trash2
import com.stefanoneve.ultimatenotes.data.db.FolderEntity
import com.stefanoneve.ultimatenotes.data.db.NoteEntity
import com.stefanoneve.ultimatenotes.ui.components.folderIcon
import com.stefanoneve.ultimatenotes.ui.components.glass
import com.stefanoneve.ultimatenotes.ui.components.themeGradient
import com.stefanoneve.ultimatenotes.ui.components.themedCard
import com.stefanoneve.ultimatenotes.ui.theme.LocalAppStyle
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalFoundationApi::class)
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

    // Pull anything the Mac wrote into the shared folder while we were away.
    LaunchedEffect(Unit) { viewModel.autoSyncIfEnabled() }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.message.value = null
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(14.dp))

            // ---- Header: serif title + settings ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Ultimate Notes",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        "${notes.size} note",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                Box(Modifier.glass(corner = 24.dp, elevation = 6.dp)) {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            Lucide.Settings2,
                            contentDescription = "Impostazioni",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ---- Glass search pill ----
            Row(
                Modifier
                    .fillMaxWidth()
                    .glass(corner = 28.dp, elevation = 6.dp)
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Lucide.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { viewModel.query.value = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(
                        MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box {
                            if (query.isEmpty()) {
                                Text(
                                    "Cerca nelle note…",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                            inner()
                        }
                    },
                )
            }

            Spacer(Modifier.height(14.dp))

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

            Spacer(Modifier.height(14.dp))

            if (notes.isEmpty()) {
                EmptyState()
            } else {
                NotesGrid(
                    notes = notes,
                    folders = folders,
                    onOpen = onOpenNote,
                    onTogglePin = viewModel::togglePin,
                    onDelete = { viewModel.deleteNote(it.id) },
                    onDuplicate = { viewModel.duplicateNote(it.id) },
                    onMove = viewModel::moveNote,
                )
            }
        }

        // ---- Gradient FAB pill ----
        val fabInteraction = remember { MutableInteractionSource() }
        val fabPressed by fabInteraction.collectIsPressedAsState()
        val fabScale by animateFloatAsState(
            targetValue = if (fabPressed) 0.92f else 1f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
            label = "fabScale",
        )
        Row(
            Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(20.dp)
                .scale(fabScale)
                .clip(RoundedCornerShape(30.dp))
                .background(themeGradient())
                .combinedClickable(
                    interactionSource = fabInteraction,
                    indication = null,
                    onClick = { viewModel.createNote(onOpenNote) },
                )
                .padding(horizontal = 22.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Lucide.Plus,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Nuova nota",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
        }

        SnackbarHost(
            snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
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
        val vaultLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocumentTree(),
        ) { uri -> uri?.let(viewModel::setVaultFolder) }
        val syncing by viewModel.syncing.collectAsState()

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
            onPickVaultFolder = { vaultLauncher.launch(null) },
            onClearVaultFolder = viewModel::clearVaultFolder,
            onSyncNow = { viewModel.syncNow() },
            syncing = syncing,
            onDismiss = { showSettings = false },
        )
    }
}

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
                icon = Lucide.StickyNote,
                selected = selectedFolderId == null,
                seed = -1,
                onClick = { onSelect(null) },
            )
        }
        items(folders, key = { it.id }) { folder ->
            FolderChip(
                label = folder.name,
                color = Color(folder.color),
                icon = folderIcon(folder.icon),
                selected = selectedFolderId == folder.id,
                seed = folder.id.hashCode(),
                onClick = { onSelect(folder.id) },
                onLongClick = { onEdit(folder) },
            )
        }
        item {
            FolderChip(
                label = "Nuova",
                color = MaterialTheme.colorScheme.outline,
                icon = Lucide.FolderPlus,
                selected = false,
                seed = -2,
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
    icon: ImageVector,
    selected: Boolean,
    seed: Int,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val style = LocalAppStyle.current
    val container by animateColorAsState(
        targetValue =
        if (selected) color else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        animationSpec = tween(220),
        label = "chipBg",
    )
    val content =
        if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.04f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "chipScale",
    )
    Row(
        modifier = Modifier
            .scale(scale)
            .themedCard(seed = seed, corner = style.corner)
            .background(container)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) content else color,
            modifier = Modifier.size(17.dp),
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
    onDuplicate: (NoteEntity) -> Unit,
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
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val cardScale by animateFloatAsState(
                targetValue = if (pressed) 0.965f else 1f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 700f),
                label = "cardScale",
            )
            Box(Modifier.animateItem()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .scale(cardScale)
                        .themedCard(seed = note.id.hashCode())
                        .combinedClickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = { onOpen(note.id) },
                            onLongClick = { menuOpen = true },
                        )
                        .padding(14.dp),
                ) {
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
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        if (note.pinned) {
                            Icon(
                                Lucide.Pin,
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
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(if (note.pinned) "Sblocca" else "Fissa in alto") },
                        leadingIcon = {
                            Icon(if (note.pinned) Lucide.PinOff else Lucide.Pin, null)
                        },
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
                                    Icon(Lucide.FolderInput, null, tint = Color(f.color))
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
                            leadingIcon = { Icon(Lucide.FolderInput, null) },
                            onClick = {
                                menuOpen = false
                                onMove(note, null)
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Duplica") },
                        leadingIcon = { Icon(Lucide.Copy, null) },
                        onClick = {
                            menuOpen = false
                            onDuplicate(note)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Elimina") },
                        leadingIcon = {
                            Icon(Lucide.Trash2, null, tint = MaterialTheme.colorScheme.error)
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
            Lucide.StickyNote,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outlineVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Nessuna nota qui.\nTocca \"Nuova nota\" per iniziare.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}
