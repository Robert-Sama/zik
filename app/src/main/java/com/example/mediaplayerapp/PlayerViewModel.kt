package com.example.mediaplayerapp

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PlayerViewModel : ViewModel() {

    private val _currentUri = MutableStateFlow<Uri?>(null)
    val currentUri: StateFlow<Uri?> = _currentUri

    private val _playlist = MutableStateFlow<List<Uri>>(emptyList())
    val playlist: StateFlow<List<Uri>> = _playlist

    private val _index = MutableStateFlow(-1)
    val index: StateFlow<Int> = _index

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    fun update(uri: Uri, playlistUris: List<Uri>, index: Int, isPlayingNow: Boolean) {
        _currentUri.value = uri
        _playlist.value = playlistUris
        _index.value = index
        _isPlaying.value = isPlayingNow
    }

    fun updatePlayingState(playing: Boolean) {
        _isPlaying.value = playing
    }
}





//// PlayerViewModel.kt
//package com.example.mediaplayerapp
//
//import androidx.lifecycle.ViewModel
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import java.io.File
//
//class PlayerViewModel : ViewModel() {
//    private val _currentFile = MutableStateFlow<File?>(null)
//    val currentFile: StateFlow<File?> = _currentFile
//
//    private val _playlist = MutableStateFlow<List<File>>(emptyList())
//    val playlist: StateFlow<List<File>> = _playlist
//
//    private val _index = MutableStateFlow(-1)
//    val index: StateFlow<Int> = _index
//
//    private val _isPlaying = MutableStateFlow(false)
//    val isPlaying: StateFlow<Boolean> = _isPlaying
//
//    fun update(file: File, list: List<File>, idx: Int, isPlayingNow: Boolean) {
//        _currentFile.value = file
//        _playlist.value = list
//        _index.value = idx
//        _isPlaying.value = isPlayingNow
//    }
//
//    fun updatePlayingState(playing: Boolean) {
//        _isPlaying.value = playing
//    }
//}
//
//
//
