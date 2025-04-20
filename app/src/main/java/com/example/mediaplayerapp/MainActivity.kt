package com.example.mediaplayerapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mediaplayerapp.MediaPlayerService
// import com.example.mediaplayerapp.PlayerStateBridge.viewModel
import com.example.mediaplayerapp.ui.theme.MediaPlayerAppTheme
import java.io.File
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts






class MainActivity : ComponentActivity() {

    private var permissionGranted = false
    lateinit var openFolderLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionGranted = hasStoragePermission()
        if (!permissionGranted) {
            requestStoragePermission()
        }


        //val mediaFiles = if (permissionGranted) getMediaFilesFromFolder() else emptyList()
        //val mediaFiles = if (permissionGranted) getMediaFilesFromFolder(this) else emptyList()
        val context = this
        //val mediaFilesState = remember { mutableStateOf<List<MediaFileItem>>(emptyList()) }



        PlaylistManager.init(this)
        openFolderLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult
                handleFolderSelection(data)
            }
        }

        setContent {
            val mediaFilesVM: MediaFilesViewModel = viewModel()
            LaunchedEffect(Unit) {
                val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
                if (prefs.getStringSet("folder_uris", emptySet())!!.isNotEmpty()) {
                    mediaFilesVM.loadMediaFiles()
                }
            }

            val mediaFiles by mediaFilesVM.mediaFiles.collectAsState()
            LaunchedEffect(permissionGranted) {
                if (permissionGranted) {
                    //mediaFilesState.value = getMediaFilesFromFolder(context)
                    mediaFilesVM.loadMediaFiles()
                }
            }
            // Dans MainActivity.kt (dans onCreate -> setContent bloc)
//            val vm: PlayerViewModel = viewModel()
//            LaunchedEffect(Unit) {
//                PlayerStateBridge.viewModel = vm
//            }
//            val viewModel = PlayerStateBridge.viewModel
//            if (viewModel == null) {
//                Box(modifier = Modifier.fillMaxSize()) {
//                    Text("Erreur : ViewModel absent", modifier = Modifier.align(Alignment.Center))
//                }
//                return
//            }

//            val vm: PlayerViewModel = viewModel()
//            LaunchedEffect(Unit) {
//                PlayerStateBridge.viewModel = vm
//                MediaPlayerService.player?.let { player ->
//                    val file = MediaPlayerService.playlist?.getOrNull(MediaPlayerService.index)
//                    val playlist = MediaPlayerService.playlist ?: emptyList()
//                    val index = MediaPlayerService.index
//                    val isPlaying = try {
//                        player.isPlaying
//                    } catch (e: IllegalStateException) {
//                        false
//                    }
//
//                    if (file != null) {
//                        viewModel.update(file, playlist, index, isPlaying)
//                    }
//                }
//            }

            // ✅ Crée et enregistre un nouveau ViewModel
            val vm: PlayerViewModel = viewModel()
            LaunchedEffect(Unit) {
                PlayerStateBridge.viewModel = vm }

            val navController = rememberNavController()
            var lastPlayedFile by rememberSaveable { mutableStateOf<String?>(null) }


            MediaPlayerAppTheme {
                //val navController = rememberNavController()
                //var lastPlayedFile by rememberSaveable { mutableStateOf<String?>(null) }

                Scaffold(
                    bottomBar = { BottomNavigationBar(navController) }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "files",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("playlists") {
                            PlaylistsPage(navController)
                        }
//                        composable("files") {
//                            MediaFileList(files = mediaFiles, navController = navController) { selectedFile ->
//                                lastPlayedFile = selectedFile.name
//
//                                val intent = Intent(this@MainActivity, MediaPlayerService::class.java).apply {
//                                    action = MediaPlayerService.ACTION_PLAY_FILE
//                                    putExtra("uri", Uri.fromFile(selectedFile).toString())
//                                }
//                                startForegroundService(intent)
//
//                                navController.navigate("player/last")
//                            }
//                        }
                        composable("files") {
                            Column {
//                                Button(onClick = {
//                                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
//                                        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
//                                    }
//                                    startActivity(intent)
//                                }) {
//                                    Text("Activer les notifications")
//                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                MediaFileList(files = mediaFiles, navController = navController) { selectedFile ->
                                    lastPlayedFile = selectedFile.name

                                    val index = mediaFiles.indexOfFirst { it.uri == selectedFile.uri }

                                    val intent = Intent(this@MainActivity, MediaPlayerService::class.java).apply {
                                        action = MediaPlayerService.ACTION_PLAY_FILE
                                        putExtra("uri", selectedFile.uri.toString())
                                        putStringArrayListExtra("playlist_uris", ArrayList(mediaFiles.map { it.uri.toString() }))
                                        putExtra("index", index)
                                    }
                                    startForegroundService(intent)

                                    navController.navigate("player/last")
                                }


//                                MediaFileList(files = mediaFiles, navController = navController) { selectedFile ->
//                                    lastPlayedFile = selectedFile.name
//
////                                    val intent = Intent(this@MainActivity, MediaPlayerService::class.java).apply {
////                                        action = MediaPlayerService.ACTION_PLAY_FILE
////                                        putExtra("uri", Uri.fromFile(selectedFile).toString())
////                                    }
////                                    startForegroundService(intent)
//                                    val index = mediaFiles.indexOf(selectedFile)
//
//                                    val intent = Intent(this@MainActivity, MediaPlayerService::class.java).apply {
//                                        action = MediaPlayerService.ACTION_PLAY_FILE
//                                        putExtra("uri", Uri.fromFile(selectedFile).toString())
//                                        putStringArrayListExtra("playlist", ArrayList(mediaFiles.map { it.absolutePath }))
//                                        putExtra("index", index)
//                                    }
//                                    startForegroundService(intent)
//
//
//                                    navController.navigate("player/last")
//                                }

                            }
                        }
                        composable("player/last") {
                            PlayerPage(lastPlayedFile)
                        }
                        composable("playlistDetails/{name}") { backStackEntry ->
                            val playlistName = backStackEntry.arguments?.getString("name") ?: ""
                            PlaylistDetailsPage(playlistName, navController)
                        }

                        composable("settings") {
                            SettingsPage()
                        }

                    }
                }
            }
        }

    }
//    @Deprecated("Use registerForActivityResult instead", level = DeprecationLevel.WARNING)
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        if (requestCode == 999 && resultCode == RESULT_OK) {
//            val vm: MediaFilesViewModel = ViewModelProvider(this)[MediaFilesViewModel::class.java]
//            vm.loadMediaFiles()
//            val treeUri = data?.data ?: return
//            // Recharge les fichiers immédiatement
//            val updatedFiles = getMediaFilesFromFolders(this)
//
////            runOnUiThread {
////                (applicationContext as? MainActivity)?.let {
////                    it.mediaFilesState.value = updatedFiles
////                }
////            }
//
//            contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
//
//            // Sauvegarde dans SharedPreferences
//            val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
//            //prefs.edit().putString("folder_uri", treeUri.toString()).apply()
//            val currentList = prefs.getStringSet("folder_uris", emptySet())!!.toMutableSet()
//            currentList.add(treeUri.toString())
//            prefs.edit().putStringSet("folder_uris", currentList).apply()
//
//        }
//    }


    private fun hasStoragePermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        ActivityCompat.requestPermissions(this, permissions, 0)
    }

    private fun handleFolderSelection(data: Intent) {
        val vm: MediaFilesViewModel = ViewModelProvider(this)[MediaFilesViewModel::class.java]
        vm.loadMediaFiles()
        val treeUri = data.data ?: return

        // Permission persistante
        contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

        // Mise à jour des préférences
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val currentList = prefs.getStringSet("folder_uris", emptySet())!!.toMutableSet()
        currentList.add(treeUri.toString())
        prefs.edit().putStringSet("folder_uris", currentList).apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        //stopService(Intent(this, MediaPlayerService::class.java))
    }

}

//fun Context.sendAction(action: String) {
//    val intent = Intent(this, MediaPlayerService::class.java).apply {
//        this.action = action
//    }
//    startService(intent)
//}
fun Context.sendAction(action: String) {
    val intent = Intent(this, MediaPlayerService::class.java).apply {
        this.action = action
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(intent)
    } else {
        startService(intent)
    }
}


//fun getMediaFilesFromFolder(): List<File> {
//    val folder = File("/storage/emulated/0/YtDown/")
//    val supportedExtensions = listOf(
//        "mp4", "mkv", "webm", "3gp", "avi",
//        "mp3", "aac", "m4a", "ogg", "oga", "flac", "wav"
//    )
//
//    return folder.listFiles()?.filter { file ->
//        file.isFile && supportedExtensions.any { ext ->
//            file.name.lowercase().endsWith(".$ext")
//        }
//    } ?: emptyList()
//}


//

fun getMediaFilesFromFolders(context: Context): List<MediaFileItem> {
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val uriStrings = prefs.getStringSet("folder_uris", emptySet()) ?: return emptyList()

    val allFiles = mutableListOf<MediaFileItem>()
    for (uriString in uriStrings) {
        val treeUri = uriString.toUri()
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)

        val cursor = context.contentResolver.query(childrenUri, arrayOf(
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DOCUMENT_ID
        ), null, null, null)

        cursor?.use {
            val nameIndex = it.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIndex = it.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val idIndex = it.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)

            while (it.moveToNext()) {
                val name = it.getString(nameIndex)
                val mime = it.getString(mimeIndex)
                val docIdChild = it.getString(idIndex)
                val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docIdChild)

                if (
                    mime != DocumentsContract.Document.MIME_TYPE_DIR &&
                    (mime.startsWith("audio/") || mime.startsWith("video/") ||
                            name.lowercase().endsWith(".mp3") || name.lowercase().endsWith(".mp4"))
                )
                {
                    allFiles.add(MediaFileItem(name, fileUri))
                }

            }
        }
    }
    return allFiles
}




// ---------- COMPOSABLES ----------

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    NavigationBar {
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("playlists") },
            label = { Text("Playlists") },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Playlists") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("files") },
            label = { Text("Fichiers") },
            icon = { Icon(Icons.Default.Folder, contentDescription = "Fichiers") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("player/last") },
            label = { Text("Lecteur") },
            icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Lecteur") }
        )
    }
}

@Composable
fun MediaFileList(files: List<MediaFileItem>, navController: NavController, onFileSelected: (MediaFileItem) -> Unit) {
    //var selectedFileForPlaylist by remember { mutableStateOf<File?>(null) }
    var selectedFileForPlaylist by remember { mutableStateOf<MediaFileItem?>(null) }

    var showDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    var showTopMenu by remember { mutableStateOf(true) }

    // Détection direction de scroll
    val previousIndex = remember { mutableIntStateOf(0) }
    val previousOffset = remember { mutableIntStateOf(0) }
    var showFilterDialog by remember { mutableStateOf(false) }

    var keyword by remember { mutableStateOf("") }
    var selectedTypes by remember { mutableStateOf(setOf("audio", "video")) }
    var sortBy by remember { mutableStateOf("name") } // ou "duration", "date"
    var ascending by remember { mutableStateOf(true) }
    var filtersActive by remember { mutableStateOf(false) }


    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (currIndex, currOffset) ->
            showTopMenu = when {
                currIndex < previousIndex.intValue -> true
                currIndex > previousIndex.intValue -> false
                currOffset < previousOffset.intValue -> true
                currOffset > previousOffset.intValue -> false
                else -> showTopMenu
            }

            previousIndex.intValue = currIndex
            previousOffset.intValue = currOffset
        }
    }


    Column(modifier = Modifier.fillMaxSize()) {
        if (showTopMenu) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val context = LocalContext.current

                IconButton(onClick = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                }) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                }

                IconButton(onClick = {
                    val shuffledFiles = files.shuffled()
                    if (shuffledFiles.isNotEmpty()) {
                        val first = shuffledFiles[0]
                        val intent = Intent(context, MediaPlayerService::class.java).apply {
                            action = MediaPlayerService.ACTION_PLAY_FILE
                            putExtra("uri", first.uri.toString())
                            putStringArrayListExtra("playlist", ArrayList(shuffledFiles.map { it.uri.toString() }))
                            //putExtra("uri", Uri.fromFile(first).toString())
                            //putStringArrayListExtra("playlist", ArrayList(shuffledFiles.map { it.uri.toString() }))

                            putExtra("index", 0)
                        }
                        context.startForegroundService(intent)
                        navController.navigate("player/last")
                    }
                }) {
                    Icon(Icons.Default.Shuffle, contentDescription = "Lecture aléatoire")
                }

                IconButton(onClick = { showFilterDialog = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filtrer")
                }

                IconButton(onClick = { navController.navigate("settings") }) {
                    Icon(Icons.Default.Settings, contentDescription = "Paramètres")
                }
            }
        }


        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(8.dp)
        ) {
            val filteredFiles = files
                .let { original ->
                    // Mot-clé actif ? -> filtrer par mot-clé
                    val keywordFiltered = if (keyword.isNotBlank()) {
                        original.filter { it.name.contains(keyword, ignoreCase = true) }
                    } else original

                    // Si filtres activés -> appliquer type + tri
                    if (filtersActive) {
                        keywordFiltered
                            .filter { file ->
                                // = file.extension.lowercase()
                                val ext = file.name.substringAfterLast('.', "").lowercase()

                                val isAudio = ext in listOf("mp3", "aac", "m4a", "ogg", "oga", "flac", "wav")
                                val isVideo = ext in listOf("mp4", "mkv", "webm", "3gp", "avi")
                                when {
                                    selectedTypes.contains("audio") && isAudio -> true
                                    selectedTypes.contains("video") && isVideo -> true
                                    else -> false
                                }
                            }
                            .sortedWith(
                                when (sortBy) {
                                    //"name" -> compareBy<File> { it.name.lowercase() }
                                    "name" -> compareBy<MediaFileItem> { it.name.lowercase() }
                                    "date" -> compareBy<MediaFileItem> { it.name } // fallback
                                    //"date" -> compareBy<MediaFileItem> { it.lastModified() }
                                    else -> compareBy<MediaFileItem> { it.name.lowercase() }
                                }.let { if (ascending) it else it.reversed() }
                            )
                    } else {
                        keywordFiltered
                    }
                }
            items(filteredFiles) { file ->
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = file.name,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onFileSelected(file)
                                },
                            style = MaterialTheme.typography.bodyLarge
                        )

                        IconButton(onClick = {
                            selectedFileForPlaylist = file
                            showDialog = true
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Ajouter à une playlist")
                        }
                    }
                    HorizontalDivider()
                }
            }


//            val filteredFiles = files
//                .filter { file ->
//                    keyword.isBlank() || file.name.contains(keyword, ignoreCase = true)
//                }
//                .filter { file ->
//                    val ext = file.extension.lowercase()
//                    val isAudio = ext in listOf("mp3", "aac", "m4a", "ogg", "oga", "flac", "wav")
//                    val isVideo = ext in listOf("mp4", "mkv", "webm", "3gp", "avi")
//                    when {
//                        selectedTypes.contains("audio") && isAudio -> true
//                        selectedTypes.contains("video") && isVideo -> true
//                        else -> false
//                    }
//                }
//                .sortedWith(
//                    when (sortBy) {
//                        "name" -> compareBy<File> { it.name.lowercase() }
//                        "date" -> compareBy<File> { it.lastModified() }
//                        else -> compareBy<File> { it.name } // fallback
//                    }.let { if (ascending) it else it.reversed() }
//                )

//            items(files) { file ->
//                Column {
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(vertical = 8.dp, horizontal = 16.dp),
//                        horizontalArrangement = Arrangement.SpaceBetween
//                    ) {
//                        Text(
//                            text = file.name,
//                            modifier = Modifier
//                                .weight(1f)
//                                .clickable {
//                                    onFileSelected(file)
//                                },
//                            style = MaterialTheme.typography.bodyLarge
//                        )
//
//                        IconButton(onClick = {
//                            selectedFileForPlaylist = file
//                            showDialog = true
//                        }) {
//                            Icon(Icons.Default.Add, contentDescription = "Ajouter à une playlist")
//                        }
//                    }
//                    HorizontalDivider()
//                }
//            }
        }

        // Dialogue d'ajout à une playlist
        if (showDialog && selectedFileForPlaylist != null) {
            AlertDialog(
                onDismissRequest = {
                    showDialog = false
                    selectedFileForPlaylist = null
                },
                title = { Text("Ajouter à une playlist") },
                text = {
                    val playlists = PlaylistManager.getAll()
                    Column {
                        if (playlists.isEmpty()) {
                            Text("Aucune playlist disponible.")
                        } else {
                            playlists.forEach { playlist ->
                                Text(
                                    text = playlist.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            PlaylistManager.addFileToPlaylist(
                                                playlist.name,
                                                //selectedFileForPlaylist!!.absolutePath
                                                selectedFileForPlaylist!!.uri.toString()

                                            )
                                            showDialog = false
                                            selectedFileForPlaylist = null
                                        }
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {}
            )
        }
    }
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    filtersActive = true
                    showFilterDialog = false
                }) {
                    Text("Appliquer")
                }
                TextButton(onClick = {
                    filtersActive = false
                    keyword = ""
                    selectedTypes = setOf("audio", "video")
                    sortBy = "name"
                    ascending = true
                    showFilterDialog = false
                }) {
                    Text("Réinitialiser")
                }

            },


//            confirmButton = {
//                TextButton(onClick = { showFilterDialog = false }) {
//                    Text("Appliquer")
//                }
//            },
            dismissButton = {
                TextButton(onClick = { showFilterDialog = false }) {
                    Text("Annuler")
                }
            },
            title = { Text("Filtres") },
            text = {
                Column {
                    OutlinedTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        label = { Text("Mot-clé") }
                    )

                    Spacer(Modifier.height(8.dp))
                    Text("Types :")
                    Row {
                        listOf("audio", "video").forEach { type ->
                            val isSelected = selectedTypes.contains(type)
                            Button(
                                onClick = {
                                    selectedTypes = if (isSelected)
                                        selectedTypes - type else selectedTypes + type
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(type)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("Tri :")
                    Row {
                        listOf("name", "date").forEach { sortOption ->
                            Button(
                                onClick = { sortBy = sortOption },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (sortBy == sortOption) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(sortOption)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row {
                        Text("Ordre : ")
                        Button(onClick = { ascending = !ascending }) {
                            Text(if (ascending) "Croissant" else "Décroissant")
                        }
                    }
                }
            }
        )
    }

}



@Composable
fun PlayerPage(filename: String?) {
    val context = LocalContext.current
    val player = MediaPlayerService.player
    val uri = MediaPlayerService.currentUri
    val vm = PlayerStateBridge.viewModel!!
    val currentUri by vm.currentUri.collectAsState()
    val playlist by vm.playlist.collectAsState()
    val index by vm.index.collectAsState()
    val isPlayingState by vm.isPlaying.collectAsState()

    val nowPlayingName = currentUri?.lastPathSegment ?: "Fichier inconnu"

    val isVideo = remember(currentUri) {
        currentUri?.let {
            val extension = it.lastPathSegment?.substringAfterLast('.')?.lowercase()
            extension in listOf("mp4", "mkv", "webm", "avi", "3gp")
        } ?: false
    }

    val upcoming = playlist.drop(index + 1)

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Lecture : $nowPlayingName",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isVideo) 300.dp else 180.dp)
        ) {
            if (isVideo && currentUri != null) {
                AndroidView(
                    factory = { ctx ->
                        android.widget.VideoView(ctx).apply {
                            setVideoURI(currentUri)
                            start()
                        }
                    }
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.Center)
                )
            }
        }

        // ⏳ Barre de progression
        val safePlayer = MediaPlayerService.player
        val (durationMs, rawPosition, progress) = try {
            val d = safePlayer?.duration?.toLong() ?: 0L
            val p = safePlayer?.currentPosition ?: 0
            val prog = if (d > 0) p.toFloat() / d else 0f
            Triple(d, p, prog)
        } catch (e: IllegalStateException) {
            Triple(0L, 0, 0f)
        }

        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(4.dp)
        )

        // 🔘 Contrôles
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = {
                context.sendAction(MediaPlayerService.ACTION_PREVIOUS)
            }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Précédent")
            }

            IconButton(onClick = {
                context.sendAction(MediaPlayerService.ACTION_REWIND_10)
            }) {
                Icon(Icons.Default.FastRewind, contentDescription = "Reculer 10s")
            }

            IconButton(onClick = {
                val isPlaying = try {
                    MediaPlayerService.player?.isPlaying == true
                } catch (e: IllegalStateException) {
                    false
                }

                if (isPlaying) {
                    context.sendAction(MediaPlayerService.ACTION_PAUSE)
                } else {
                    context.sendAction(MediaPlayerService.ACTION_PLAY)
                }
            }) {
                Icon(
                    if (isPlayingState) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Lecture/Pause"
                )
            }

            IconButton(onClick = {
                val stopIntent = Intent(context, MediaPlayerService::class.java).apply {
                    action = MediaPlayerService.ACTION_STOP
                }
                context.startService(stopIntent)
            }) {
                Icon(Icons.Default.Delete, contentDescription = "Arrêter")
            }

            IconButton(onClick = {
                context.sendAction(MediaPlayerService.ACTION_FORWARD_10)
            }) {
                Icon(Icons.Default.FastForward, contentDescription = "Avancer 10s")
            }

            IconButton(onClick = {
                context.sendAction(MediaPlayerService.ACTION_NEXT)
            }) {
                Icon(Icons.Default.SkipNext, contentDescription = "Suivant")
            }
        }

        // 🎶 Prochaines pistes
        Text(
            text = "🎵 À venir",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            items(upcoming) { upcomingUri ->
                Text(
                    text = upcomingUri.lastPathSegment ?: "Inconnu",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                HorizontalDivider()
            }
        }
    }
}






@Composable
fun PlaylistsPage(navController: NavController) {
    val context = LocalContext.current
    var playlistName by remember { mutableStateOf("") }
    var playlists by remember { mutableStateOf(PlaylistManager.getAll()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "📁 Vos playlists",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Champ de création
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = playlistName,
                onValueChange = { playlistName = it },
                label = { Text("Nouvelle playlist") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (playlistName.isNotBlank()) {
                    val success = PlaylistManager.createPlaylist(playlistName.trim())
                    if (success) {
                        playlists = PlaylistManager.getAll()
                        playlistName = ""
                    }
                }
            }) {
                Text("Créer")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn {
            items(playlists) { playlist ->
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable {
                            navController.navigate("playlistDetails/${playlist.name}")
                        }
                )
                HorizontalDivider()
            }
        }
    }
}


//fun getMediaFilesFromFolder(): List<File> {
//    val folder = File("/storage/emulated/0/YtDown/")
//    val supportedExtensions = listOf(
//        "mp4", "mkv", "webm", "3gp", "avi",
//        "mp3", "aac", "m4a", "ogg", "oga", "flac", "wav"
//    )
//
//    return folder.listFiles()?.filter { file ->
//        file.isFile && supportedExtensions.any { ext ->
//            file.name.lowercase().endsWith(".$ext")
//        }
//    } ?: emptyList()
//}

//fun Context.sendAction(context: Context, action: String) {
//    val intent = Intent(context, MediaPlayerService::class.java).apply {
//        this.action = action
//    }
//    context.startService(intent)
//}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

@Composable
fun PlaylistDetailsPage(playlistName: String, navController: NavController) {
    val context = LocalContext.current
    var playlist by remember { mutableStateOf(PlaylistManager.getPlaylist(playlistName)) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "🎵 Playlist : $playlistName",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Button(
            onClick = { showDeleteDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text("🗑 Supprimer cette playlist")
        }

        // ▶️ Lire toute la playlist
        Button(
            onClick = {
                playlist?.let {
                    if (it.files.isNotEmpty()) {
                        val intent = Intent(context, MediaPlayerService::class.java).apply {
                            action = MediaPlayerService.ACTION_PLAY_FILE
                            putExtra("uri", it.files[0])
                            putStringArrayListExtra("playlist_uris", ArrayList(it.files))
                            putExtra("index", 0)
                        }
                        context.startForegroundService(intent)
                        navController.navigate("player/last")
                    }
                }
            },
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text("▶️ Lire la playlist")
        }

        LazyColumn {
            playlist?.files?.forEach { filePath ->
                item {
                    val uri = filePath.toUri()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = uri.lastPathSegment ?: "Fichier inconnu",
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            PlaylistManager.removeFileFromPlaylist(playlistName, filePath)
                            playlist = PlaylistManager.getPlaylist(playlistName)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer")
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    PlaylistManager.deletePlaylist(playlistName)
                    showDeleteDialog = false
                    navController.navigate("playlists") {
                        popUpTo("playlists") { inclusive = true }
                    }
                }) {
                    Text("Confirmer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Annuler")
                }
            },
            title = { Text("Supprimer cette playlist ?") },
            text = { Text("Cette action est irréversible.") }
        )
    }
}



@Composable
fun SettingsPage() {
    val context = LocalContext.current
    val activity = context as? Activity
    val uriState = rememberSaveable { mutableStateOf<Uri?>(null) }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Text("📂 Paramètres", style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            //activity?.startActivityForResult(intent, 999)
            (activity as? MainActivity)?.openFolderLauncher?.launch(intent)

        }) {
            Text("Choisir un dossier")
        }
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val selectedFolders = prefs.getStringSet("folder_uris", emptySet())?.toList() ?: emptyList()

        selectedFolders.forEach { uriStr ->
            val uri = uriStr.toUri()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = uri.lastPathSegment ?: uriStr, modifier = Modifier.weight(1f))
                Button(onClick = {
                    val newSet = selectedFolders.toMutableSet()
                    newSet.remove(uriStr)
                    prefs.edit().putStringSet("folder_uris", newSet).apply()
                    // Recharger les fichiers
                    (context as? MainActivity)?.let {
                        //val vm = ViewModelProvider(it)[MediaFilesViewModel::class.java]
                        //vm.loadMediaFiles()
                    }
                }) {
                    Text("Retirer")
                }
            }
            val vm = ViewModelProvider(context as MainActivity)[MediaFilesViewModel::class.java]
            vm.loadMediaFiles()

        }
        val vm = ViewModelProvider(context as MainActivity)[MediaFilesViewModel::class.java]
        LaunchedEffect(Unit) {
            vm.loadMediaFiles()
        }


        uriState.value?.let {
            Spacer(Modifier.height(8.dp))
            Text("📁 Dossier sélectionné : ${it.path}")
        }
    }
}



