package de.jupiterpi.cloudful.cli

import picocli.CommandLine.*
import java.awt.Desktop
import java.net.URI
import kotlin.io.path.Path

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
            println("Syncing repository ${repository.repositoryId} ...\n")
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
        val url = "https://console.cloud.google.com/storage/browser/$BUCKET/$REPOSITORIES_ROOT${repository.repositoryId}/${repository.openPath}"

        Desktop.getDesktop().browse(URI(url))
    }
}

@Command(name = "init", description = ["Creates a new repository."])
class InitCommand : Runnable {
    @Parameters(index = "0", paramLabel = "<repo id>")
    lateinit var repositoryId: String

    @Parameters(index = "1", paramLabel = "<directory>")
    lateinit var directoryName: String

    override fun run() = initializeRepository(repositoryId, Path(directoryName))
}

@Command(name = "clone", description = ["Creates a local copy of a repository in cloud."])
class CloneCommand : Runnable {
    @Parameters(index = "0", paramLabel = "<repo id>")
    lateinit var repositoryId: String

    @Parameters(index = "1", paramLabel = "<directory>")
    lateinit var directoryName: String

    override fun run() = cloneRepository(repositoryId, Path(directoryName))
}

@Command(name = "force", description = ["Force download or upload."])
class ForceCommand : Runnable {
    @Parameters(index = "0", paramLabel = "<download|upload>")
    lateinit var mode: String

    override fun run() {
        val repository = readRepository() ?: throw DisplayException("No repository found!")
        if (mode != "download" && mode != "upload") throw DisplayException("Invalid mode: $mode")
        println("Forcing $mode of repository ${repository.repositoryId}...")
        syncOperation(repository, mode == "upload")
    }
}

@Command(name = "registry", description = ["Opens the global repository registry."])
class RegistryCommand : Runnable {
    override fun run() {
        println("Opening $repositoryRegistry")
        Desktop.getDesktop().edit(repositoryRegistry.toFile())
    }
}