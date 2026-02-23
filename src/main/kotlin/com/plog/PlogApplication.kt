package com.plog

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PlogApplication

fun main(args: Array<String>) {
    runApplication<PlogApplication>(*args)
}