import com.beust.kobalt.buildScript
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.RepositoryBuilder
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

val bs = buildScript {
    buildFileClasspath("org.eclipse.jgit:org.eclipse.jgit:jar:4.7.0.201704051617-r")
}

fun gitVersion(): String {
    val version =
        cmd("git describe --tags --always --first-parent")?.trim('\n')
            ?: "UNKNOWN${UUID.randomUUID()}"

    val repo = RepositoryBuilder().findGitDir().build()
    val clean = Git.wrap(repo).status().call().isClean

    val dirty = if (!clean) ".dirty" else ""

    return "$version$dirty"
}

fun cmd(command: String, workingDir: File = File(".")): String? {
    try {
        val parts = command.split("\\s".toRegex())

        val proc = ProcessBuilder(*parts.toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        proc.waitFor(60, TimeUnit.MINUTES)
        return proc.inputStream.bufferedReader().readText()
    } catch(e: IOException) {
        e.printStackTrace()
        return null
    }
}