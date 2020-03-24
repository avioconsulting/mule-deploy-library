package com.avioconsulting.mule.deployment

import com.avioconsulting.mule.deployment.api.ILogger

class TestConsoleLogger implements ILogger {
    @Override
    def println(String message) {
        System.out.println(message)
    }
}
