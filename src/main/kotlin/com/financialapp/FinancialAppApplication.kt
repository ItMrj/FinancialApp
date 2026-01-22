package com.financialapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FinancialAppApplication

fun main(args: Array<String>) {
    runApplication<FinancialAppApplication>(*args)
}
