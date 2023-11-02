package de.jupiterpi.cloudful.cli

import java.nio.file.Path
import kotlin.io.path.*

fun syncRepository(repository: Repository) {
    val excludePattern = repository.excludePaths
        .map { it.replace("\\", "\\\\").replace(".", "\\.").replace("*", ".*") }
        .joinToString(separator = "|") { "(^$it$)" }
    val excludeStr = if (excludePattern.isBlank()) "" else "-x \"$excludePattern\""
    val cmd = "gsutil -m rsync -r -d $excludeStr ${repository.root} gs://$BUCKET$REPOSITORIES_ROOT/${repository.repositoryPath}"
    println("> $cmd")
    Runtime.getRuntime().exec("cmd.exe /c $cmd").let {
        it.inputStream.transferTo(System.out)
        it.errorStream.transferTo(System.err)
    }
}

fun syncAllRepositories() {
    repositoryRegistry.readLines().filter { it.isNotBlank() }.forEach { repositoryRoot ->
        println()
        val repository = readRepository(Path(repositoryRoot))!!
        println("Syncing repository ${repository.repositoryPath} at ${repository.root} ...")
        syncRepository(repository)
    }
}

fun createRepository(repositoryPath: String) {
    if (readRepository() != null) throw DisplayException("There's already a repository here!")
    Path(".cloudful").createFile().writeText("> $repositoryPath\n\n# Exclude files here...")
}

@OptIn(ExperimentalPathApi::class)
fun readRepository(path: Path = Path("").absolute()): Repository? {
    var repositoryRoot = path
    while (!(repositoryRoot / ".cloudful").exists()) {
        repositoryRoot = repositoryRoot.parent ?: return null
    }

    val configurationLines = (repositoryRoot / ".cloudful").readLines()

    val repositoryPath = configurationLines.first { it.startsWith(">") }.substring(1).trim()
    val openPath = repositoryRoot.relativize(Path("").absolute()).toString().replace("\\", "/")

    val excludePaths = configurationLines
        .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith(">")}
        .map { it.trim() }
        .map { if (!it.contains(Regex("(!|\\*)")) && (repositoryRoot / it).isDirectory()) "$it\\*" else it }
        .toMutableList()

    repositoryRoot.walk().filter { it.fileName.toString() == ".cloudignore" }
        .forEach { file -> excludePaths += file.readLines().filter { !it.startsWith("#") }.map { "${repositoryRoot.relativize(file.parent)}\\${it.trim()}" } }

    return Repository(repositoryRoot, repositoryPath, excludePaths, openPath)
}

data class Repository(
    val root: Path,
    val repositoryPath: String,
    val excludePaths: List<String>,
    val openPath: String,
)