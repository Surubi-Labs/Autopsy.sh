package dev.autopsy

import dev.autopsy.cli.CliRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import kotlin.system.exitProcess

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    if (args.isNotEmpty() && args[0] == "analyze") {
        // CLI mode: no Spring context, direct execution
        val exitCode = CliRunner().run(args)
        exitProcess(exitCode)
    }

    // Server mode: Spring Boot with full infrastructure
    runApplication<Application>(*args)
}
