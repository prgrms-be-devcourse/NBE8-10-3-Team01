package com.plog

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
object PlogApplication {
    @JvmStatic
    fun main(args: Array<String>) {
        SpringApplication.run(PlogApplication::class.java, *args)
    }
}
