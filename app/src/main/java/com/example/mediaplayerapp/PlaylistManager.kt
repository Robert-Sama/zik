package com.example.mediaplayerapp

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

data class Playlist(
    val name: String,
    val files: MutableList<String> = mutableListOf()
)

object PlaylistManager {
    private const val PLAYLIST_FILE_NAME = "playlists.json"
    private val gson = Gson()
    private var playlists: MutableList<Playlist> = mutableListOf()

    private lateinit var playlistFile: File

    fun init(context: Context) {
        playlistFile = File(context.filesDir, PLAYLIST_FILE_NAME)
        if (playlistFile.exists()) {
            val type = object : TypeToken<MutableList<Playlist>>() {}.type
            playlists = gson.fromJson(playlistFile.readText(), type) ?: mutableListOf()
        } else {
            playlists = mutableListOf()
            save()
        }
    }

    fun getAll(): List<Playlist> = playlists

    fun getPlaylist(name: String): Playlist? = playlists.find { it.name == name }

    fun createPlaylist(name: String): Boolean {
        if (playlists.any { it.name == name }) return false
        playlists.add(Playlist(name))
        save()
        return true
    }

    fun deletePlaylist(name: String): Boolean {
        val removed = playlists.removeIf { it.name == name }
        if (removed) save()
        return removed
    }

    fun addFileToPlaylist(name: String, filePath: String): Boolean {
        val playlist = getPlaylist(name) ?: return false
        if (!playlist.files.contains(filePath)) {
            playlist.files.add(filePath)
            save()
        }
        return true
    }

    fun removeFileFromPlaylist(name: String, filePath: String): Boolean {
        val playlist = getPlaylist(name) ?: return false
        val removed = playlist.files.remove(filePath)
        if (removed) save()
        return removed
    }

    private fun save() {
        playlistFile.writeText(gson.toJson(playlists))
    }
}
