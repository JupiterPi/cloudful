package de.jupiterpi.cloudful.cli

import com.google.common.collect.Iterators
import org.apache.commons.codec.digest.DigestUtils
import java.io.ByteArrayInputStream
import java.io.SequenceInputStream
import kotlin.io.path.*

@OptIn(ExperimentalPathApi::class)
fun syncRepository(repository: Repository) {
    // fetch cloud version
    val cloudConfigurationText = storage.get(BUCKET, "$REPOSITORIES_ROOT${repository.repositoryId}/.cloudful").getContent().decodeToString()
    val cloudVersion = cloudConfigurationText.split(Regex("[\\r\\n]+"))[0].substring(1)

    // calculate checksum of local changes
    val checksumInputStreams = repository.root.walk()
        .filterNot { path -> repository.excludePaths.any { path.startsWith(it) } }
        .filterNot { it == (repository.root / ".cloudful") }
        .map { it.inputStream() }
        .toMutableList()
    checksumInputStreams += ByteArrayInputStream((repository.root / ".cloudful").readText().substringAfter("\n").toByteArray())
    val localChecksum = DigestUtils.md5Hex(SequenceInputStream(Iterators.asEnumeration(checksumInputStreams.iterator())))

    // decide whether to upload or download
    val upload = cloudVersion == repository.versionDefinition
    val download = repository.checksum == localChecksum

    // resolve special cases
    if (upload && download) {
        println("There are no changes.")
        return
    }
    if (!upload && !download) {
        println("There's a sync conflict!")
        println("Resolve it or try `cloudful force upload` or `cloudful force download`")
        return
    }

    if (upload) println("Uploading repository (-> ${repository.numericVersion+1})...")
    if (download) println("Downloading repository (${repository.numericVersion} -> ${cloudVersion.substringBefore("_")})...")

    // update checksum
    if (upload) {
        val configurationFile = (repository.root / ".cloudful")
        configurationFile.writeLines(
            configurationFile.readLines().toMutableList().also { it[0] = "#${repository.numericVersion+1}_$localChecksum" }
        )
    }

    syncOperation(repository, upload)
}

fun syncOperation(repository: Repository, upload: Boolean) {
    val excludeStr = repository.excludePaths
        .map { it.replace("\\", "\\\\").replace(".", "\\.").replace("*", ".*") }
        .joinToString(separator = "|") { "(^$it$)" }
        .let { if (it.isBlank()) "" else "-x \"$it\"" }

    if (upload) executeCommand("gsutil -m rsync -r -d $excludeStr ${repository.root} gs://$BUCKET/$REPOSITORIES_ROOT${repository.repositoryId}")
    else executeCommand("gsutil -m rsync -r -d $excludeStr gs://$BUCKET/$REPOSITORIES_ROOT${repository.repositoryId} ${repository.root}")
}
