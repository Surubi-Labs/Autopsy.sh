package dev.autopsy

import dev.autopsy.cli.CliRunner
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val exitCode = CliRunner().run(args)
    exitProcess(exitCode)
}
