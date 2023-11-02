package de.jupiterpi.cloudful.cli

import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.awt.Desktop
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.readLines
import kotlin.system.exitProcess

lateinit var repositoryRegistry: Path

fun main(args: Array<String>) {
    val dataDirectory = Path(args[0])
    repositoryRegistry = dataDirectory / "repos.txt"

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
    CommandLine.HelpCommand::class,
    SyncCommand::class,
    OpenCommand::class,
    InitCommand::class,
    RegistryCommand::class,
])
class CloudfulCommand

@Command(name = "sync", description = ["Syncs repositories (by default, the current repository)."])
class SyncCommand : Runnable {
    @Option(names = ["-a", "--all"], description = ["Sync all globally registered repositories."])
    var all: Boolean = false

    override fun run() {
        if (!all) {
            val repository = readRepository() ?: throw DisplayException("No repository found!")
            println("Syncing repository ${repository.repositoryPath} ...\n")
            syncRepository(repository)
        } else {
            println("Syncing all repositories...")
            syncAllRepositories()
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
    @Parameters(index = "0", paramLabel = "<repo path>")
    lateinit var repositoryPath: String

    override fun run() = createRepository(repositoryPath)
}

@Command(name = "registry", description = ["Opens the global repository registry."])
class RegistryCommand : Runnable {
    override fun run() {
        println("Opening $repositoryRegistry")
        Desktop.getDesktop().edit(repositoryRegistry.toFile())
    }
}