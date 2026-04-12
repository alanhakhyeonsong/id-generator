package me.ramos.idgenerator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class IdGeneratorApplication

fun main(args: Array<String>) {
    runApplication<IdGeneratorApplication>(*args)
}
