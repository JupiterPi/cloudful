package de.jupiterpi.cloudful.cli

import com.google.cloud.storage.StorageOptions
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.HelpCommand
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.notExists
import kotlin.io.path.writeText
import kotlin.system.exitProcess

val storage = StorageOptions.getDefaultInstance().service!!

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
    CloneCommand::class,
    RegistryCommand::class,
])
class CloudfulCommand