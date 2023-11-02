package de.jupiterpi.cloudful.cli

import picocli.CommandLine
import picocli.CommandLine.Command
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val exitCode = CommandLine(CloudfulCommand()).execute(*args)
    exitProcess(exitCode)
}

@Command(name = "cloudful", subcommands = [
    CommandLine.HelpCommand::class,
    SyncCommand::class,
])
class CloudfulCommand

@Command(name = "sync", description = ["Syncs the current repository."])
class SyncCommand : Runnable {
    override fun run() {
        println("Syncing...")
        sync()
    }
}