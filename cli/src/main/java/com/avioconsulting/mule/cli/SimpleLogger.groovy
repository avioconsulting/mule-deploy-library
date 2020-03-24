package com.avioconsulting.mule.cli

import com.avioconsulting.mule.deployment.api.ILogger

class SimpleLogger implements ILogger {
    @Override
    def println(String message) {
        System.out.println(message)
    }
}
