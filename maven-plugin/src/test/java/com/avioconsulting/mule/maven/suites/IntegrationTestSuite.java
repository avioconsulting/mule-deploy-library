package com.avioconsulting.mule.maven.suites;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages("com.avioconsulting.mule.maven.integrationtest")
public class IntegrationTestSuite {
}
