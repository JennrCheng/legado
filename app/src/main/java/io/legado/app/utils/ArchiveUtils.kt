package io.legado.app.utils

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.utils.compress.RarUtils
import io.legado.app.utils.compress.SevenZipUtils
import io.legado.app.utils.compress.ZipUtils
import splitties.init.appCtx
import java.io.File

/* 自动判断压缩文件后缀 然后再调用具体的实现 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
object ArchiveUtils {

    const val TEMP_FOLDER_NAME = "ArchiveTemp"
    // 临时目录 下次启动自动删除
    val TEMP_PATH: String by lazy {
        appCtx.externalCache.getFile(TEMP_FOLDER_NAME).createFolderReplace().absolutePath
    }

    fun deCompress(
        archiveUri: Uri,
        path: String = TEMP_PATH,
        filter: ((String) -> Boolean)? = null
    ): List<File> {
        return deCompress(FileDoc.fromUri(archiveUri, false), path, filter)
    }

    fun deCompress(
        archivePath: String,
        path: String = TEMP_PATH,
        filter: ((String) -> Boolean)? = null
    ): List<File> {
        return deCompress(Uri.parse(archivePath), path, filter)
    }

    fun deCompress(
        archiveFile: File,
        path: String = TEMP_PATH,
        filter: ((String) -> Boolean)? = null
    ): List<File> {
        return deCompress(FileDoc.fromFile(archiveFile), path, filter)
    }

    fun deCompress(
        archiveDoc: DocumentFile,
        path: String = TEMP_PATH,
        filter: ((String) -> Boolean)? = null
    ): List<File> {
        return deCompress(FileDoc.fromDocumentFile(archiveDoc), path, filter)
    }

    fun deCompress(
        archiveFileDoc: FileDoc,
        path: String = TEMP_PATH,
        filter: ((String) -> Boolean)? = null
    ): List<File> {
        if (archiveFileDoc.isDir) throw IllegalArgumentException("Unexpected Folder input")
        val name = archiveFileDoc.name
        checkAchieve(name)
        val workPathFileDoc = getCacheFolderFileDoc(name, path)
        val workPath = workPathFileDoc.toString()
        return archiveFileDoc.openInputStream().getOrThrow().use {
            when {
                name.endsWith(".zip", ignoreCase = true) -> ZipUtils.unZipToPath(it, workPath, filter)
                name.endsWith(".rar", ignoreCase = true) -> RarUtils.unRarToPath(it, workPath, filter)
                name.endsWith(".7z", ignoreCase = true) -> SevenZipUtils.un7zToPath(it, workPath, filter)
                else -> throw IllegalArgumentException("Unexpected archive format")
            }
        }
    }

    /* 遍历目录获取文件名 */
    fun getArchiveFilesName(fileUri: Uri, filter: ((String) -> Boolean)? = null): List<String> = getArchiveFilesName(FileDoc.fromUri(fileUri, false), filter)
 
    fun getArchiveFilesName(
        fileDoc: FileDoc,
        filter: ((String) -> Boolean)? = null
    ): List<String> {
        val name = fileDoc.name
        checkAchieve(name)
        return fileDoc.openInputStream().getOrThrow().use {
            when {
                name.endsWith(".rar", ignoreCase = true) -> {
                    RarUtils.getFilesName(it, filter)
                }
                name.endsWith(".zip", ignoreCase = true) -> {
                    ZipUtils.getFilesName(it, filter)
                }
                name.endsWith(".7z", ignoreCase = true) -> {
                    SevenZipUtils.getFilesName(it, filter)
                }
                else -> emptyList()
           }
        }
    }

    fun checkAchieve(name: String) {
        if (
            name.endsWith(".zip", ignoreCase = true) ||
            name.endsWith(".rar", ignoreCase = true) ||
            name.endsWith(".7z", ignoreCase = true)
        ) return
        throw IllegalArgumentException("Unexpected file suffix: Only 7z rar zip Accepted")
    }

    private fun getCacheFolderFileDoc(
        archiveName: String,
        workPath: String
    ): FileDoc {
        return FileDoc.fromUri(Uri.parse(workPath), true)
            .createFolderIfNotExist(MD5Utils.md5Encode16(archiveName))
    }
}