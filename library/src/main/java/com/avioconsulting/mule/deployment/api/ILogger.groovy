package com.avioconsulting.mule.deployment.api

// Jenkins, etc. may need more primitive logging methods than something like Log4j2, so will just declare
// a real simple interface that can be implemented easily using a variety of methods (System.out, etc.)
interface ILogger {
    def println(String message)
    def error(String message)
}
