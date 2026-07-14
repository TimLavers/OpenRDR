package io.rippledown.integration.utils

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import io.rippledown.simpleapi.SimpleInterpreterLoader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path

fun main(args: Array<String>) {
    val zipPath = args[0]
    val map = mutableMapOf<String,String>()
    var idx = 1
    while (idx < args.size - 1) {
        map[args[idx]] = args[idx + 1]
        idx += 2
    }
    val interpreterLoaderClass = Class.forName("io.rippledown.standalone.KbLoader").kotlin
    val constructor = interpreterLoaderClass.constructors.firstOrNull { constructor ->
        constructor.parameters.size == 1 && constructor.parameters.first().type.classifier == Path::class
    }!!
    val interpreterLoader = constructor.call(Path.of(zipPath)) as SimpleInterpreterLoader
    val interpreter = interpreterLoader.getInterpreter()
    val received = interpreter.interpretStringMap(map)
    print(">>")
    print(received)
    println("<<")
}

fun runMapThroughInterpreterBuiltFromZip(map: Map<String,String>, exportedZip: File, jarFile: File): String {
    val programArgs = arrayListOf<String>()
    programArgs.add(exportedZip.absolutePath)
    map.forEach { (left, right) ->
        programArgs.add(left)
        programArgs.add(right)
    }

    val program = """
        import io.rippledown.simpleapi.SimpleInterpreterLoader
        import java.nio.file.Path

        fun main(args: Array<String>) {
            val zipPath = args[0]
            val map = mutableMapOf<String,String>()
            var idx = 1
            while (idx < args.size - 1) {
                map[args[idx]] = args[idx + 1]
                idx += 2
            }
            val interpreterLoaderClass = Class.forName("io.rippledown.standalone.KbLoader").kotlin
            val constructor = interpreterLoaderClass.constructors.firstOrNull { constructor ->
                constructor.parameters.size == 1 && constructor.parameters.first().type.classifier == Path::class
            }!!
            val interpreterLoader = constructor.call(Path.of(zipPath)) as SimpleInterpreterLoader
            val interpreter = interpreterLoader.getInterpreter()
            val received = interpreter.interpretStringMap(map)
            print(">>")
            print(received)
            println("<<")
        }
    """.trimIndent().lines()
    val jarFilePath = jarFile.absolutePath
    val allOutput = runMainAgainstIsolatedJar(program, jarFilePath, programArgs.toTypedArray())
    return allOutput.substringAfter(">>").substringBeforeLast("<<")
}

fun runMainAgainstIsolatedJar(codeLines: List<String>, jarFilePath: String, programArgs: Array<String> = emptyArray()): String {
    val jarFile = File(jarFilePath)
    if (!jarFile.exists()) throw IllegalArgumentException("JAR not found: $jarFilePath")

    // Create a temp directory with a fixed-name source file so Kotlin compiles it to "TestAppKt"
    val tempDir = Files.createTempDirectory("kotlin-source").toFile().apply { deleteOnExit() }
    val sourceFile = File(tempDir, "TestApp.kt").apply {
        writeText(codeLines.joinToString("\n"))
        deleteOnExit()
    }
    val outputDir = Files.createTempDirectory("compiled-output").toFile().apply {
        deleteOnExit()
    }

    val compilerArgs = arrayOf(
        sourceFile.absolutePath,
        "-cp", jarFile.absolutePath,
        "-d", outputDir.absolutePath
    )

    val exitCode = K2JVMCompiler().exec(System.err, *compilerArgs)
    if (exitCode != ExitCode.OK) {
        throw RuntimeException("Compilation failed with code: $exitCode")
    }

    val isolatedClassLoader = URLClassLoader(
        arrayOf(outputDir.toURI().toURL(), jarFile.toURI().toURL()),
        ClassLoader.getPlatformClassLoader()
    )

    val mainClass = isolatedClassLoader.loadClass("TestAppKt")
    val mainMethod = mainClass.getMethod("main", Array<String>::class.java)

    val outputStream = ByteArrayOutputStream()
    val originalOut = System.out
    System.setOut(PrintStream(outputStream))

    try {
        mainMethod.invoke(null, programArgs)
    } finally {
        System.setOut(originalOut)
    }

    return outputStream.toString().trim()
}
