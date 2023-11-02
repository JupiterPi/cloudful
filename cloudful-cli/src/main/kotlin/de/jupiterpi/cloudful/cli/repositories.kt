package de.jupiterpi.cloudful.cli

import java.nio.file.Path
import kotlin.io.path.*

fun sync(repository: Repository) {
    val excludePattern = repository.excludePaths
        .map { it.replace("\\", "\\\\").replace(".", "\\.").replace("*", ".*") }
        .joinToString(separator = "|") { "(^$it$)" }
    val cmd = "gsutil -m rsync -r -d -x \"$excludePattern\" ${repository.root} gs://$BUCKET$REPOSITORIES_ROOT/${repository.repositoryPath}"
    println("> $cmd")
    Runtime.getRuntime().exec("cmd.exe /c $cmd").let {
        it.inputStream.transferTo(System.out)
        it.errorStream.transferTo(System.err)
    }
}

@OptIn(ExperimentalPathApi::class)
fun readRepository(): Repository {
    var repositoryRoot = Path("").absolute()
    while (!(repositoryRoot / ".cloudful").exists()) {
        repositoryRoot = repositoryRoot.parent
    }

    val configurationLines = (repositoryRoot / ".cloudful").readLines()

    val repositoryPath = configurationLines.first { it.startsWith(">") }.substring(1).trim()
    val openPath = repositoryRoot.relativize(Path("").absolute()).toString().replace("\\", "/")

    val excludePaths = configurationLines
        .filter { it.isNotEmpty() && !it.startsWith(">") }
        .map { it.trim() }
        .map { if (!it.contains(Regex("(!|\\*)")) && (repositoryRoot / it).isDirectory()) "$it\\*" else it }
        .toMutableList()

    repositoryRoot.walk().filter { it.fileName.toString() == ".cloudignore" }
        .forEach { file -> excludePaths += file.readLines().map { "${repositoryRoot.relativize(file.parent)}\\${it.trim()}" } }

    return Repository(repositoryRoot, repositoryPath, excludePaths, openPath)
}

data class Repository(
    val root: Path,
    val repositoryPath: String,
    val excludePaths: List<String>,
    val openPath: String,
)