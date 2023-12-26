package com.avioconsulting.mule.cli.suites;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages("com.avioconsulting.mule.cli.integrationtest")
public class IntegrationTestSuite {
}
