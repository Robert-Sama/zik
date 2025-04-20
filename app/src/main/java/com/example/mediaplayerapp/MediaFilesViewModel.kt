package com.example.mediaplayerapp

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MediaFilesViewModel(application: Application) : AndroidViewModel(application) {

    private val _mediaFiles = MutableStateFlow<List<MediaFileItem>>(emptyList())
    val mediaFiles: StateFlow<List<MediaFileItem>> = _mediaFiles.asStateFlow()

    fun loadMediaFiles() {
        val context = getApplication<Application>().applicationContext
        _mediaFiles.value = getMediaFilesFromFolders(context)
    }

}
