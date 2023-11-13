package de.jupiterpi.cloudful.cli

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import picocli.CommandLine.*
import java.awt.Desktop
import java.net.URI
import kotlin.io.path.*

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

    override fun run() {
        val directory = Path(directoryName)
        if (directory.exists()) throw DisplayException("That directory already exists!")

        val exists = storage.list(BUCKET, Storage.BlobListOption.prefix("$REPOSITORIES_ROOT$repositoryId"), Storage.BlobListOption.pageSize(1)).values.iterator().hasNext()
        if (exists) throw DisplayException("Repository already exists in cloud!")

        directory.createDirectory()
        val configurationText = "#0_00\n> $repositoryId\n\n# Exclude files here..."
        (directory / ".cloudful").createFile().writeText(configurationText)
        storage.create(
            BlobInfo.newBuilder(BlobId.of(BUCKET, "$REPOSITORIES_ROOT$repositoryId/.cloudful")).setContentType("text/plain").build(),
            configurationText.toByteArray()
        )
    }
}

@Command(name = "registry", description = ["Opens the global repository registry."])
class RegistryCommand : Runnable {
    override fun run() {
        println("Opening $repositoryRegistry")
        Desktop.getDesktop().edit(repositoryRegistry.toFile())
    }
}