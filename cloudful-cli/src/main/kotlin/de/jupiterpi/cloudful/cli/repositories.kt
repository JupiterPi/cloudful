package de.jupiterpi.cloudful.cli

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*

@OptIn(ExperimentalPathApi::class)
fun syncAllRepositories(uploadOnly: Boolean) {
    var repositories = repositoryRegistry.readLines().filter { it.isNotBlank() }.mapNotNull { readRepository(Path(it)) }
    repositoryRegistry.writeLines(repositories.map { it.root.toString() })

    if (uploadOnly) {
        val lastSynced = Date(lastSyncedFile.readText().trim().toLong())
        repositories = repositories
            .filter {
                for (path in it.root.walk()) {
                    if (path.toFile().lastModified() > lastSynced.time) return@filter true
                }
                false
            }
    }

    repositories.forEach { repository ->
        println("\nSyncing repository ${repository.repositoryId} at ${repository.root} ...")

        try { syncRepository(repository) }
        catch (e: Exception) { exceptionHandler(e) }
    }

    lastSyncedFile.writeText(Date().time.toString())
}

fun initializeRepository(repositoryId: String, directory: Path) {
    if (directory.exists()) throw DisplayException("That directory already exists!")

    val exists = storage.list(BUCKET, Storage.BlobListOption.prefix("$REPOSITORIES_ROOT$repositoryId"), Storage.BlobListOption.pageSize(1)).values.iterator().hasNext()
    if (exists) throw DisplayException("Repository already exists in cloud!")

    println("Initializing new repository $repositoryId in $directory...")

    directory.createDirectory()
    val configurationText = "#0_00\n> $repositoryId\n\n# Exclude files here..."
    (directory / ".cloudful").createFile().writeText(configurationText)
    storage.create(
        BlobInfo.newBuilder(BlobId.of(BUCKET, "$REPOSITORIES_ROOT$repositoryId/.cloudful")).setContentType("text/plain").build(),
        configurationText.toByteArray()
    )
}

fun cloneRepository(repositoryId: String, directory: Path) {
    if (directory.exists()) throw DisplayException("That directory already exists!")

    val exists = storage.list(BUCKET, Storage.BlobListOption.prefix("$REPOSITORIES_ROOT$repositoryId"), Storage.BlobListOption.pageSize(1)).values.iterator().hasNext()
    if (!exists) throw DisplayException("Repository doesn't exists in cloud!")

    println("Cloning $repositoryId into $directory...")

    directory.createDirectory()
    executeCommand("gsutil -m cp -r gs://$BUCKET/$REPOSITORIES_ROOT$repositoryId/* $directory")

    val repositories = repositoryRegistry.readLines().filter { it.isNotBlank() }
    val root = Path("").absolute().toString()
    if (!repositories.contains(root)) {
        repositoryRegistry.writeLines(repositories + root)
        println("Added it to the global registry.")
    }
}

@OptIn(ExperimentalPathApi::class)
fun readRepository(path: Path = Path("").absolute()): Repository? {
    var repositoryRoot = path
    while (!(repositoryRoot / ".cloudful").exists()) {
        repositoryRoot = repositoryRoot.parent ?: return null
    }

    val configurationLines = (repositoryRoot / ".cloudful").readLines()

    val versionDefinition = configurationLines[0].substring(1).split("_")
    val numericVersion = versionDefinition[0].toInt()
    val checksum = versionDefinition[1]

    val definition = configurationLines.first { it.startsWith(">") }.substring(1).trim().split(Regex(" +"))
    val repositoryId = definition[0]
    // val tags = definition[1].substring(1, definition[1].length-1).split(Regex(", *")) // unused

    val openPath = repositoryRoot.relativize(Path("").absolute()).toString().replace("\\", "/")

    val excludePaths = configurationLines
        .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith(">")}
        .map { it.trim() }
        .map { if (!it.contains(Regex("(!|\\*)")) && (repositoryRoot / it).isDirectory()) "$it\\*" else it }
        .toMutableList()

    repositoryRoot.walk().filter { it.fileName.toString() == ".cloudignore" }
        .forEach { file -> excludePaths += file.readLines().filter { !it.startsWith("#") }.map { "${repositoryRoot.relativize(file.parent)}\\${it.trim()}" } }

    return Repository(repositoryRoot, repositoryId, numericVersion, checksum, excludePaths, openPath)
}

data class Repository(
    val root: Path,
    val repositoryId: String,
    val numericVersion: Int,
    val checksum: String,
    val excludePaths: List<String>,
    val openPath: String,
) {
    val versionDefinition get() = "${numericVersion}_$checksum"
}