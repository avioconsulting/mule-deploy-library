package com.avioconsulting.mule.integrationtest

import com.avioconsulting.mule.deployment.internal.models.AppStatus
import com.avioconsulting.mule.deployment.internal.models.AppStatusPackage
import com.avioconsulting.mule.deployment.internal.models.DeploymentItem
import com.avioconsulting.mule.deployment.internal.subdeployers.BaseDeployer
import com.avioconsulting.mule.deployment.internal.subdeployers.CloudHubDeployer
import com.avioconsulting.mule.deployment.internal.subdeployers.CloudHubV2Deployer
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.HttpClientBuilder

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

class TestUtils {

    static def hitEndpointAndAssert(String username,
                                    String password,
                                    String url,
                                    String stringToAssert) {
        def credsProvider = new BasicCredentialsProvider().with {
            setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(username,
                            password))
            it
        }
        HttpClientBuilder.create()
                .setDefaultCredentialsProvider(credsProvider)
                .build().withCloseable { client ->
            Throwable exception = null
            10.times {
                try {
                    def request = new HttpGet(url)
                    println "Hitting app @ ${request}"
                    client.execute(request).withCloseable { response ->
                        assertThat response.statusLine.statusCode,
                                is(equalTo(200))
                        assertThat stringToAssert,
                                is(equalTo(response.entity.content.text))
                        exception = null
                    }

                }
                catch (AssertionError e) {
                    println 'Test failed, waiting 500 ms and trying again'
                    exception = e
                    Thread.sleep(500)
                }
            }
            if (exception) {
                throw exception
            } else {
                println 'test passed'
            }
        }
    }

    static waitForAppDeletion(BaseDeployer deployer,
                              String environment,
                              String appName,
                              String groupId) {
        def tries = 0
        def deleted = false
        def failed = false
        println 'Now checking to see if app has been deleted'
        while (!deleted && tries < 10) {
            tries++
            println "*** Try ${tries} ***"
            AppStatusPackage status

            if (deployer instanceof CloudHubDeployer) {
                CloudHubDeployer cloudHubDeployer = deployer
                status = cloudHubDeployer.getAppStatus(environment,
                        appName)
            } else if (deployer instanceof CloudHubV2Deployer) {
                CloudHubV2Deployer cloudHubV2Deployer = deployer
                DeploymentItem appInfo = cloudHubV2Deployer.getAppInfo(environment, groupId, appName)
                status = deployer.getAppStatus(appInfo)
            }

            println "Received status of ${status}"
            if (status.appStatus == AppStatus.NotFound) {
                println 'App removed successfully!'
                deleted = true
                break
            }
            def retryIntervalInMs = 10000
            println "Sleeping for ${retryIntervalInMs / 1000} seconds and will recheck..."
            Thread.sleep(retryIntervalInMs)
        }
        if (!deleted && failed) {
            throw new Exception('Deletion failed on 1 or more nodes. Please see logs and messages as to why app did not start')
        }
        if (!deleted) {
            throw new Exception("Deletion has not completed after ${tries} tries!")
        }
    }

}
