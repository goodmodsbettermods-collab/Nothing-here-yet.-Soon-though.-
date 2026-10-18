package com.goodmods.questmodfilestorage

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    private val defaultRoot = File(Environment.getExternalStorageDirectory(), "Mods")
    private var httpServer: SimpleHttpServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!defaultRoot.exists()) defaultRoot.mkdirs()

        setContent {
            val context = LocalContext.current
            var root by remember { mutableStateOf(defaultRoot) }
            var files by remember { mutableStateOf(listFiles(root)) }
            var showGrant by remember { mutableStateOf(!hasAllFilesAccess()) }

            LaunchedEffect(root) { files = listFiles(root) }

            Scaffold(topBar = {
                TopAppBar(title = { Text("Quest Mod Storage") })
            }) { padding ->
                Column(modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()) {

                    if (showGrant) {
                        Card(modifier = Modifier.padding(8.dp)) {
                            Column(Modifier.padding(8.dp)) {
                                Text("All files access is required to manage mods on shared storage.")
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { requestAllFilesAccess(this@MainActivity) }) {
                                    Text("Grant All Files Access")
                                }
                            }
                        }
                    }

                    Row(Modifier.padding(8.dp)) {
                        Button(onClick = {
                            httpServer = SimpleHttpServer(root)
                            httpServer?.start()
                            Toast.makeText(context, "HTTP upload server started on :8080 (PUT /upload?name=FILENAME)", Toast.LENGTH_LONG).show()
                        }) { Text("Start Upload Server") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            httpServer?.stop()
                            httpServer = null
                            Toast.makeText(context, "HTTP server stopped", Toast.LENGTH_SHORT).show()
                        }) { Text("Stop Server") }
                    }

                    Text("Path: ${root.absolutePath}", modifier = Modifier.padding(8.dp))

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(files) { f ->
                            FileRow(f, onRefresh = { files = listFiles(root) })
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        httpServer?.stop()
    }

    private fun listFiles(dir: File): List<File> = dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))?.toList() ?: emptyList()

    private fun hasAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else true
    }

    private fun requestAllFilesAccess(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:${activity.packageName}")
                activity.startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                activity.startActivity(intent)
            }
        }
    }
}

@Composable
fun FileRow(file: File, onRefresh: () -> Unit) {
    val context = LocalContext.current
    var showDetails by remember { mutableStateOf(false) }

    Card(modifier = Modifier
        .padding(6.dp)
        .fillMaxWidth()) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(if (file.isDirectory) "[DIR] ${file.name}" else file.name, modifier = Modifier.weight(1f))
                Button(onClick = { if (file.isDirectory) { /* open */ } else { /* preview */ } }) { Text("Open") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    if (file.isDirectory) deleteRecursive(file) else file.delete()
                    onRefresh()
                }) { Text("Delete") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    // simple rename demo
                    val newName = file.name + "_renamed"
                    file.renameTo(File(file.parentFile, newName))
                    onRefresh()
                }) { Text("Rename") }
            }

            if (showDetails) {
                Text("Path: ${file.absolutePath}")
                Text("Size: ${if (file.isDirectory) "-" else file.length()} bytes")
            }
        }
    }
}

fun deleteRecursive(fileOrDirectory: File) {
    if (fileOrDirectory.isDirectory) {
        val children = fileOrDirectory.listFiles() ?: return
        for (child in children) deleteRecursive(child)
    }
    fileOrDirectory.delete()
}
