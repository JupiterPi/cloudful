package de.jupiterpi.cloudful.cli

import picocli.CommandLine
import picocli.CommandLine.*
import java.awt.Desktop
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.system.exitProcess

lateinit var repositoryRegistry: Path
lateinit var lastSyncedFile: Path

fun initializeFiles(dataPath: String) {
    val dataDirectory = Path(dataPath)
    repositoryRegistry = dataDirectory / "repos.txt"
    lastSyncedFile = dataDirectory / "last_synced.txt"

    if (repositoryRegistry.notExists()) repositoryRegistry.writeText("")
    if (lastSyncedFile.notExists()) lastSyncedFile.writeText("0")
}

fun main(args: Array<String>) {
    initializeFiles(args[0])

    val exitCode = CommandLine(CloudfulCommand())
        .setExecutionExceptionHandler { e, _, _ ->
            if (e is DisplayException) println("Error: ${e.message}") else e.printStackTrace()
            1
        }
        .execute(*(args.drop(1).toTypedArray()))
    exitProcess(exitCode)
}

class DisplayException(msg: String) : Exception(msg)

@Command(name = "cloudful", subcommands = [
    HelpCommand::class,
    SyncCommand::class,
    OpenCommand::class,
    InitCommand::class,
    RegistryCommand::class,
])
class CloudfulCommand

@Command(name = "sync", description = ["Syncs repositories (by default, the current repository)."])
class SyncCommand : Runnable {
    @Option(names = ["-a", "--all"], description = ["Sync all globally registered repositories."])
    var all = false

    @Option(names = ["-u", "--upload-only"], description = ["Only attempt sync when there are local changes (use only with --all)."])
    var uploadOnly = false

    override fun run() {
        if (uploadOnly && !all) println("Only use --upload-only in combination with --all!")
        if (!all) {
            val repository = readRepository() ?: throw DisplayException("No repository found!")
            println("Syncing repository ${repository.repositoryPath} ...\n")
            syncRepository(repository)
        } else {
            println("Syncing all repositories${if (uploadOnly) " (upload only)" else ""}...")
            syncAllRepositories(uploadOnly)
        }
    }
}

@Command(name = "open", description = ["Opens the current directory in the Cloud Console."])
class OpenCommand : Runnable {
    override fun run() {
        val repository = readRepository() ?: throw DisplayException("No repository found!")
        val url = "https://console.cloud.google.com/storage/browser/$BUCKET$REPOSITORIES_ROOT/${repository.repositoryPath}/${repository.openPath}"

        Desktop.getDesktop().browse(URI(url))
    }
}

@Command(name = "init", description = ["Initialize the current directory as a new repository."])
class InitCommand : Runnable {
    @Parameters(paramLabel = "<repo path>", arity = "0..1")
    var repositoryPath: String? = null

    override fun run() = createRepository(repositoryPath)
}

@Command(name = "registry", description = ["Opens the global repository registry."])
class RegistryCommand : Runnable {
    override fun run() {
        println("Opening $repositoryRegistry")
        Desktop.getDesktop().edit(repositoryRegistry.toFile())
    }
}