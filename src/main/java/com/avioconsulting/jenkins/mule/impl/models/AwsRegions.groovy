package com.avioconsulting.jenkins.mule.impl.models

enum AwsRegions {
    UsEast1('us-east-1'),
    UsEast2('us-east-2'),
    UsWest1('us-west-1'),
    UsWest2('us-west-2')

    String awsCode

    AwsRegions(String awsCode) {
        this.awsCode = awsCode
    }
}
