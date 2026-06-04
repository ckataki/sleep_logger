package com.noom.interview.fullstack.sleep

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@Suppress("UtilityClassWithPublicConstructor")
@SpringBootApplication
class SleepApplication {
    companion object {
        const val UNIT_TEST_PROFILE = "unittest"
    }
}

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    runApplication<SleepApplication>(*args)
}
