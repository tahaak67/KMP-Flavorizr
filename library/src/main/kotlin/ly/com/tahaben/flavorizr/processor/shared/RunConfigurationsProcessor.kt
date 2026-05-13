package ly.com.tahaben.flavorizr.processor.shared

import ly.com.tahaben.flavorizr.model.FlavorConfig
import ly.com.tahaben.flavorizr.processor.AbstractProcessor
import java.io.File

/**
 * Generates per-flavor IntelliJ IDEA / Android Studio Gradle run configurations
 * under the root project's .idea/runConfigurations/ for a non-mobile target
 * (desktop or web). IntelliJ only reads run configurations from the root .idea/
 * folder, so writing them inside the consumer module would make them invisible
 * to the IDE.
 *
 * Each generated configuration invokes the target's configured Gradle task in
 * the consumer module (via externalProjectPath) and sets the active flavor as
 * an environment variable, using the key configured via
 * `kmpFlavorizr { envVariableKey = "..." }`.
 *
 * The processor is idempotent — files are overwritten on every run.
 */
class RunConfigurationsProcessor(
    private val target: Target,
    private val rootProjectDir: File,
    private val moduleRelativePath: String,
) : AbstractProcessor() {

    enum class Target(val displayName: String, val instruction: String) {
        DESKTOP("Desktop", "runConfig:desktop"),
        WEB("Web", "runConfig:web"),
    }

    override val name: String = target.instruction

    override fun execute(config: FlavorConfig, projectDir: File) {
        val runTask = when (target) {
            Target.DESKTOP -> config.globalConfig.desktop?.takeIf { it.generateRunConfigurations }?.runTask
            Target.WEB -> config.globalConfig.web?.takeIf { it.generateRunConfigurations }?.runTask
        } ?: return

        if (config.flavors.isEmpty()) {
            log("No flavors configured, skipping ${target.displayName} run configurations")
            return
        }

        val envKey = config.environmentVariableKey
        val outDir = File(rootProjectDir, ".idea/runConfigurations")
        outDir.mkdirs()

        val externalProjectPath = if (moduleRelativePath.isEmpty()) {
            "\$PROJECT_DIR\$"
        } else {
            "\$PROJECT_DIR\$/$moduleRelativePath"
        }

        for (flavorName in config.flavors.keys) {
            val configName = "${target.displayName} $flavorName"
            val fileName = "${target.displayName}_${sanitizeFileName(flavorName)}.xml"
            val xml = generateXml(
                configName = configName,
                runTask = runTask,
                envKey = envKey,
                envValue = flavorName,
                externalProjectPath = externalProjectPath,
            )
            File(outDir, fileName).writeText(xml)
            log("Generated .idea/runConfigurations/$fileName")
        }
    }

    private fun generateXml(
        configName: String,
        runTask: String,
        envKey: String,
        envValue: String,
        externalProjectPath: String,
    ): String = buildString {
        appendLine("""<component name="ProjectRunConfigurationManager">""")
        appendLine("""  <configuration default="false" name="${esc(configName)}" type="GradleRunConfiguration" factoryName="Gradle">""")
        appendLine("""    <ExternalSystemSettings>""")
        appendLine("""      <option name="env">""")
        appendLine("""        <map>""")
        appendLine("""          <entry key="${esc(envKey)}" value="${esc(envValue)}" />""")
        appendLine("""        </map>""")
        appendLine("""      </option>""")
        appendLine("""      <option name="executionName" />""")
        appendLine("""      <option name="externalProjectPath" value="${esc(externalProjectPath)}" />""")
        appendLine("""      <option name="externalSystemIdString" value="GRADLE" />""")
        appendLine("""      <option name="scriptParameters" value="" />""")
        appendLine("""      <option name="taskDescriptions">""")
        appendLine("""        <list />""")
        appendLine("""      </option>""")
        appendLine("""      <option name="taskNames">""")
        appendLine("""        <list>""")
        appendLine("""          <option value="${esc(runTask)}" />""")
        appendLine("""        </list>""")
        appendLine("""      </option>""")
        appendLine("""      <option name="vmOptions" />""")
        appendLine("""    </ExternalSystemSettings>""")
        appendLine("""    <ExternalSystemDebugServerProcess>true</ExternalSystemDebugServerProcess>""")
        appendLine("""    <ExternalSystemReattachDebugProcess>true</ExternalSystemReattachDebugProcess>""")
        appendLine("""    <DebugAllEnabled>false</DebugAllEnabled>""")
        appendLine("""    <RunAsTest>false</RunAsTest>""")
        appendLine("""    <method v="2" />""")
        appendLine("""  </configuration>""")
        appendLine("""</component>""")
    }

    private fun esc(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun sanitizeFileName(value: String): String =
        value.replace(Regex("""[^A-Za-z0-9._-]"""), "_")
}
