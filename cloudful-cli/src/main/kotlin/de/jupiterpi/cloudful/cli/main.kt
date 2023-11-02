package de.jupiterpi.cloudful.cli

import picocli.CommandLine
import picocli.CommandLine.Command
import java.awt.Desktop
import java.net.URI
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val exitCode = CommandLine(CloudfulCommand()).execute(*args)
    exitProcess(exitCode)
}

@Command(name = "cloudful", subcommands = [
    CommandLine.HelpCommand::class,
    SyncCommand::class,
    OpenCommand::class,
])
class CloudfulCommand

@Command(name = "sync", description = ["Syncs the current repository."])
class SyncCommand : Runnable {
    override fun run() {
        val repository = readRepository()
        println("Syncing repository ${repository.repositoryPath} ...\n")
        sync(repository)
    }
}

@Command(name = "open", description = ["Opens the current directory in the Cloud Console."])
class OpenCommand : Runnable {
    override fun run() {
        val repository = readRepository()
        val url = "https://console.cloud.google.com/storage/browser/$BUCKET$REPOSITORIES_ROOT/${repository.repositoryPath}/${repository.openPath}"

        // see https://stackoverflow.com/a/5226244/13164753
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
        }
    }
}