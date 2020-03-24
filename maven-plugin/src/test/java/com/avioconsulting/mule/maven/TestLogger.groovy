package com.avioconsulting.mule.maven

import org.apache.maven.plugin.logging.SystemStreamLog

class TestLogger extends SystemStreamLog {
    List<String> errors = []

    @Override
    void error(CharSequence content) {
        super.error(content)
        errors << content.toString()
    }

    @Override
    boolean isDebugEnabled() {
        true
    }
}
