package me.ramos.idgenerator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class IdGeneratorApplication

fun main(args: Array<String>) {
    runApplication<IdGeneratorApplication>(*args)
}
