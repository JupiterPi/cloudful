package de.jupiterpi.cloudful.cli

import java.nio.file.Path
import java.util.*
import kotlin.io.path.*

fun syncRepository(repository: Repository) {
    val excludePattern = repository.excludePaths
        .map { it.replace("\\", "\\\\").replace(".", "\\.").replace("*", ".*") }
        .joinToString(separator = "|") { "(^$it$)" }
    val excludeStr = if (excludePattern.isBlank()) "" else "-x \"$excludePattern\""
    val cmd = "gsutil -m rsync -r -d $excludeStr ${repository.root} gs://$BUCKET$REPOSITORIES_ROOT/${repository.repositoryId}"
    println("> $cmd")
    Runtime.getRuntime().exec("cmd.exe /c $cmd").let {
        it.inputStream.transferTo(System.out)
        it.errorStream.transferTo(System.err)
    }
}

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
        syncRepository(repository)
    }

    lastSyncedFile.writeText(Date().time.toString())
}

fun createRepository(repositoryPath: String?) {
    if (readRepository() != null) {
        println("There's already a repository here!")
    } else {
        if (repositoryPath.isNullOrBlank()) throw Exception("Need to provide a repository path!")
        Path(".cloudful").createFile().writeText("> $repositoryPath\n\n# Exclude files here...")
        println("Created new repository. See .cloudful")
    }

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
    val tags = definition[1].split(Regex(", *")) // unused

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