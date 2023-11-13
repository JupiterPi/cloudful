package de.jupiterpi.cloudful.cli

fun executeCommand(cmd: String) {
    println("> $cmd")
    Runtime.getRuntime().exec("cmd.exe /c $cmd").let {
        it.inputStream.transferTo(System.out)
        it.errorStream.transferTo(System.err)
    }
}