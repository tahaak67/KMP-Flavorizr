package ly.com.tahaben.flavorizr.processor

import ly.com.tahaben.flavorizr.model.FlavorConfig
import java.io.File

interface Processor {
    val name: String
    fun execute(config: FlavorConfig, projectDir: File)
}

abstract class AbstractProcessor : Processor {
    protected fun log(message: String) {
        println("[kmp-flavorizr] $name: $message")
    }
}

/**
 * A processor that transforms the content of a file (String -> String).
 */
abstract class StringProcessor : AbstractProcessor() {
    abstract fun transform(input: String, config: FlavorConfig): String
}

/**
 * A processor that reads a file, transforms it, and writes it back.
 */
abstract class FileStringProcessor(
    private val relativePath: String,
    private val createIfMissing: Boolean = false,
) : AbstractProcessor() {

    abstract fun transform(input: String, config: FlavorConfig): String

    override fun execute(config: FlavorConfig, projectDir: File) {
        val file = File(projectDir, relativePath)

        if (!file.exists()) {
            if (createIfMissing) {
                file.parentFile?.mkdirs()
                file.createNewFile()
                log("Created new file: $relativePath")
            } else {
                log("File not found, skipping: $relativePath")
                return
            }
        }

        val original = file.readText()
        val transformed = transform(original, config)
        file.writeText(transformed)
        log("Modified: $relativePath")
    }
}

/**
 * A processor that creates a new file with generated content.
 */
abstract class NewFileProcessor : AbstractProcessor() {

    abstract fun filePath(config: FlavorConfig, projectDir: File): File
    abstract fun content(config: FlavorConfig): String

    override fun execute(config: FlavorConfig, projectDir: File) {
        val file = filePath(config, projectDir)
        file.parentFile?.mkdirs()
        file.writeText(content(config))
        log("Generated: ${file.relativeTo(projectDir)}")
    }
}

/**
 * Composite processor that executes a list of sub-processors sequentially.
 */
class QueueProcessor(
    override val name: String,
    private val processors: List<Processor>,
) : AbstractProcessor() {

    override fun execute(config: FlavorConfig, projectDir: File) {
        log("Starting queue (${processors.size} processors)")
        for (processor in processors) {
            processor.execute(config, projectDir)
        }
        log("Queue complete")
    }
}

/**
 * A processor that runs a shell command.
 */
class ShellProcessor(
    override val name: String,
    private val commandBuilder: (FlavorConfig, File) -> List<String>,
    private val workingDirBuilder: ((FlavorConfig, File) -> File)? = null,
) : AbstractProcessor() {

    override fun execute(config: FlavorConfig, projectDir: File) {
        val command = commandBuilder(config, projectDir)
        val workDir = workingDirBuilder?.invoke(config, projectDir) ?: projectDir

        log("Running: ${command.joinToString(" ")}")

        val process = ProcessBuilder(command)
            .directory(workDir)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            throw RuntimeException("$name failed (exit code $exitCode):\n$output")
        }

        if (output.isNotBlank()) {
            log(output.trim())
        }
    }
}
