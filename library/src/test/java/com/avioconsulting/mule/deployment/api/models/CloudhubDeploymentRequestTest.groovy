package com.avioconsulting.mule.deployment.api.models

import com.avioconsulting.mule.MavenInvoke
import com.avioconsulting.mule.deployment.api.models.deployment.CloudhubDeploymentRequest
import org.junit.BeforeClass
import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

class CloudhubDeploymentRequestTest implements MavenInvoke {
    @BeforeClass
    static void setup() {
        buildApp()
    }

    @Test
    void explicit() {
        // arrange
        def file = new File('src/test/resources/some_file.txt')

        // act
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('4.2.2'),
                                                    file,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client',
                                                    'new-app',
                                                    '1.2.3')

        // assert
        request.with {
            assertThat appName,
                       is(equalTo('new-app'))
            assertThat normalizedAppName,
                       is(equalTo('client-new-app-dev'))
            assertThat appVersion,
                       is(equalTo('1.2.3'))
        }
    }

    @Test
    void derived_app_version_and_name_normal() {
        // arrange

        // act
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest(),
                                                    builtFile,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client')

        // assert
        request.with {
            assertThat appName,
                       is(equalTo('mule-deploy-lib-v4-test-app'))
            assertThat normalizedAppName,
                       is(equalTo('client-mule-deploy-lib-v4-test-app-dev'))
            assertThat appVersion,
                       is(equalTo('2.2.9'))
            assertThat 'app.runtime in the POM',
                       workerSpecRequest.muleVersion,
                       is(equalTo('4.3.0'))
        }
    }

    @Test
    void derived_app_version_hf2() {
        // arrange
        buildApp('4.1.4-hf2')
        try {

            // act
            def request = new CloudhubDeploymentRequest('DEV',
                                                        new CloudhubWorkerSpecRequest(),
                                                        builtFile,
                                                        'theKey',
                                                        'theClientId',
                                                        'theSecret',
                                                        'client')

            // assert
            assertThat 'app.runtime in the POM',
                       request.workerSpecRequest.muleVersion,
                       is(equalTo('4.1.4'))
        }
        finally {
            // need to restore our 'normal' version as to not break the other tests
            buildApp()
        }
    }

    @Test
    void spaces_in_name() {
        // arrange
        def file = new File('src/test/resources/some_file.txt')

        // act
        def exception = shouldFail {
            new CloudhubDeploymentRequest('DEV',
                                          new CloudhubWorkerSpecRequest('3.9.1',
                                                                        false,
                                                                        1,
                                                                        WorkerTypes.Micro,
                                                                        AwsRegions.UsEast1),
                                          file,
                                          'theKey',
                                          'theClientId',
                                          'theSecret',
                                          'client',
                                          'some app name',
                                          '1.2.3')
        }

        // assert
        assertThat exception.message,
                   is(equalTo("Runtime Manager does not like spaces in app names and you specified 'some app name'!"))
    }

    @Test
    void getCloudhubAppInfo_no_region() {
        // arrange

        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest(),
                                                    builtFile,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client')

        // act
        def appInfo = request.getCloudhubAppInfo()

        // assert
        assertThat appInfo,
                   is(equalTo([
                           domain                   : 'client-mule-deploy-lib-v4-test-app-dev',
                           muleVersion              : [
                                   version: '4.3.0'
                           ],
                           monitoringAutoRestart    : true,
                           workers                  : [
                                   type  : [
                                           name: 'Micro'
                                   ],
                                   amount: 1
                           ],
                           staticIPsEnabled         : false,
                           loggingCustomLog4JEnabled: false,
                           objectStoreV1            : false,
                           persistentQueues         : false,
                           properties               : [
                                   env                                               : 'dev',
                                   'crypto.key'                                      : 'theKey',
                                   'anypoint.platform.client_id'                     : 'theClientId',
                                   'anypoint.platform.client_secret'                 : 'theSecret',
                                   'anypoint.platform.config.analytics.agent.enabled': true
                           ]
                   ]))
    }

    @Test
    void getCloudhubAppInfo_new_app_property_overrides_runtime_manager() {
        // arrange
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest(null,
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    builtFile,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client',
                                                    null,
                                                    null,
                                                    [prop1: 'foo', prop2: 'bar'])

        // act
        def appInfo = request.getCloudhubAppInfo()

        // assert
        assertThat appInfo,
                   is(equalTo([
                           domain                   : 'client-mule-deploy-lib-v4-test-app-dev',
                           muleVersion              : [
                                   version: '4.3.0'
                           ],
                           region                   : 'us-east-1',
                           monitoringAutoRestart    : true,
                           workers                  : [
                                   type  : [
                                           name: 'Micro'
                                   ],
                                   amount: 1
                           ],
                           staticIPsEnabled         : false,
                           loggingCustomLog4JEnabled: false,
                           objectStoreV1            : false,
                           persistentQueues         : false,
                           properties               : [
                                   env                                               : 'dev',
                                   'crypto.key'                                      : 'theKey',
                                   'anypoint.platform.client_id'                     : 'theClientId',
                                   'anypoint.platform.client_secret'                 : 'theSecret',
                                   'anypoint.platform.config.analytics.agent.enabled': true,
                                   prop1                                             : 'foo',
                                   prop2                                             : 'bar'
                           ]

                   ]))
    }

    @Test
    void getCloudhubAppInfo_persistent_queues() {
        // arrange
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest(null,
                                                                                  true,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  AwsRegions.UsEast1),
                                                    builtFile,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client')

        // act
        def appInfo = request.getCloudhubAppInfo()

        // assert
        assertThat appInfo,
                   is(equalTo([
                           domain                   : 'client-mule-deploy-lib-v4-test-app-dev',
                           muleVersion              : [
                                   version: '4.3.0'
                           ],
                           region                   : 'us-east-1',
                           monitoringAutoRestart    : true,
                           workers                  : [
                                   type  : [
                                           name: 'Micro'
                                   ],
                                   amount: 1
                           ],
                           staticIPsEnabled         : false,
                           loggingCustomLog4JEnabled: false,
                           objectStoreV1            : false,
                           persistentQueues         : true,
                           properties               : [
                                   env                                               : 'dev',
                                   'crypto.key'                                      : 'theKey',
                                   'anypoint.platform.client_id'                     : 'theClientId',
                                   'anypoint.platform.client_secret'                 : 'theSecret',
                                   'anypoint.platform.config.analytics.agent.enabled': true
                           ]
                   ]))
    }

    @Test
    void getCloudhubAppInfo_other_props() {
        // arrange
        def otherProperties = [
                persistentQueues: true,
                properties      : [
                        env: 'TST'
                ]
        ] as Map<String, String>
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest(),
                                                    builtFile,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client',
                                                    null,
                                                    null,
                                                    [:],
                                                    otherProperties)

        // act
        def appInfo = request.getCloudhubAppInfo()

        // assert
        assertThat appInfo,
                   is(equalTo([
                           domain                   : 'client-mule-deploy-lib-v4-test-app-dev',
                           muleVersion              : [
                                   version: '4.3.0'
                           ],
                           monitoringAutoRestart    : true,
                           workers                  : [
                                   type  : [
                                           name: 'Micro'
                                   ],
                                   amount: 1
                           ],
                           staticIPsEnabled         : false,
                           loggingCustomLog4JEnabled: false,
                           objectStoreV1            : false,
                           persistentQueues         : true,
                           properties               : [
                                   env                                               : 'TST',
                                   'crypto.key'                                      : 'theKey',
                                   'anypoint.platform.client_id'                     : 'theClientId',
                                   'anypoint.platform.client_secret'                 : 'theSecret',
                                   'anypoint.platform.config.analytics.agent.enabled': true
                           ]
                   ]))
    }

    @Test
    void getCloudhubAppInfo_other_props_only_ch_settings() {
        // arrange
        def otherProperties = [
                persistentQueues: true
        ] as Map<String, String>
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest(),
                                                    builtFile,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client',
                                                    null,
                                                    null,
                                                    [:],
                                                    otherProperties)

        // act
        def appInfo = request.getCloudhubAppInfo()

        // assert
        assertThat appInfo,
                   is(equalTo([
                           domain                   : 'client-mule-deploy-lib-v4-test-app-dev',
                           muleVersion              : [
                                   version: '4.3.0'
                           ],
                           monitoringAutoRestart    : true,
                           workers                  : [
                                   type  : [
                                           name: 'Micro'
                                   ],
                                   amount: 1
                           ],
                           staticIPsEnabled         : false,
                           loggingCustomLog4JEnabled: false,
                           objectStoreV1            : false,
                           persistentQueues         : true,
                           properties               : [
                                   env                                               : 'dev',
                                   'crypto.key'                                      : 'theKey',
                                   'anypoint.platform.client_id'                     : 'theClientId',
                                   'anypoint.platform.client_secret'                 : 'theSecret',
                                   'anypoint.platform.config.analytics.agent.enabled': true
                           ]
                   ]))
    }

    @Test
    void getCloudhubAppInfo_other_props_both() {
        // arrange
        def otherProperties = [
                persistentQueues: true,
                properties      : [
                        env  : 'TST',
                        prop1: 'should not see this'
                ]
        ] as Map<String, String>
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest(),
                                                    builtFile,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client',
                                                    null,
                                                    null,
                                                    [prop1: 'foo', prop2: 'bar'],
                                                    otherProperties)

        // act
        def appInfo = request.getCloudhubAppInfo()

        // assert
        assertThat appInfo,
                   is(equalTo([
                           domain                   : 'client-mule-deploy-lib-v4-test-app-dev',
                           muleVersion              : [
                                   version: '4.3.0'
                           ],
                           monitoringAutoRestart    : true,
                           workers                  : [
                                   type  : [
                                           name: 'Micro'
                                   ],
                                   amount: 1
                           ],
                           staticIPsEnabled         : false,
                           loggingCustomLog4JEnabled: false,
                           objectStoreV1            : false,
                           persistentQueues         : true,
                           properties               : [
                                   env                                               : 'TST',
                                   'crypto.key'                                      : 'theKey',
                                   'anypoint.platform.client_id'                     : 'theClientId',
                                   'anypoint.platform.client_secret'                 : 'theSecret',
                                   'anypoint.platform.config.analytics.agent.enabled': true,
                                   prop1                                             : 'foo',
                                   prop2                                             : 'bar'
                           ]
                   ]))
    }

    @Test
    void getCloudhubAppInfo_upper_case() {
        // arrange
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest('3.9.1'),
                                                    builtFile,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client',
                                                    'NEW-APP',
                                                    '1.2.3')

        // act
        def appInfo = request.getCloudhubAppInfo()

        // assert
        assertThat appInfo,
                   is(equalTo([
                           domain                   : 'client-new-app-dev',
                           muleVersion              : [
                                   version: '3.9.1'
                           ],
                           monitoringAutoRestart    : true,
                           workers                  : [
                                   type  : [
                                           name: 'Micro'
                                   ],
                                   amount: 1
                           ],
                           staticIPsEnabled         : false,
                           loggingCustomLog4JEnabled: false,
                           objectStoreV1            : false,
                           persistentQueues         : false,
                           properties               : [
                                   env                                               : 'dev',
                                   'crypto.key'                                      : 'theKey',
                                   'anypoint.platform.client_id'                     : 'theClientId',
                                   'anypoint.platform.client_secret'                 : 'theSecret',
                                   'anypoint.platform.config.analytics.agent.enabled': true
                           ]

                   ]))
    }

    @Test
    void getCloudhubAppInfo_optional_props() {
        // arrange
        def request = new CloudhubDeploymentRequest('DEV',
                                                    new CloudhubWorkerSpecRequest(null,
                                                                                  false,
                                                                                  1,
                                                                                  WorkerTypes.Micro,
                                                                                  null,
                                                                                  'abcdefg',
                                                                                  true,
                                                                                  true,
                                                                                  false),
                                                    builtFile,
                                                    'theKey',
                                                    'theClientId',
                                                    'theSecret',
                                                    'client',
                                                    null,
                                                    null,
                                                    [:],
                                                    [:],
                                                    false)
        // act
        def appInfo = request.getCloudhubAppInfo()

        // assert
        assertThat appInfo,
                   is(equalTo([
                           domain                   : 'client-mule-deploy-lib-v4-test-app-dev',
                           muleVersion              : [
                                   version : '4.3.0',
                                   updateId: 'abcdefg'
                           ],
                           monitoringAutoRestart    : true,
                           workers                  : [
                                   type  : [
                                           name: 'Micro'
                                   ],
                                   amount: 1
                           ],
                           staticIPsEnabled         : true,
                           loggingCustomLog4JEnabled: true,
                           objectStoreV1            : true,
                           persistentQueues         : false,
                           properties               : [
                                   env                              : 'dev',
                                   'crypto.key'                     : 'theKey',
                                   'anypoint.platform.client_id'    : 'theClientId',
                                   'anypoint.platform.client_secret': 'theSecret'
                           ]
                   ]))
    }

    @Test
    void test_obfuscate_properties() {
        def request = new CloudhubDeploymentRequest('DEV',
                new CloudhubWorkerSpecRequest(),
                builtFile,
                'theKey',
                'theClientId',
                'theSecret',
                'client')

        // act
        def appInfo = request.getCloudAppInfoAsObfuscatedJson()

        // assert
        assertThat appInfo,
                is(equalTo([
                        domain                   : 'client-mule-deploy-lib-v4-test-app-dev',
                        muleVersion              : [
                                version: '4.3.0'
                        ],
                        monitoringAutoRestart    : true,
                        workers                  : [
                                type  : [
                                        name: 'Micro'
                                ],
                                amount: 1
                        ],
                        staticIPsEnabled         : false,
                        loggingCustomLog4JEnabled: false,
                        objectStoreV1            : false,
                        persistentQueues         : false,
                        properties               : [
                                env                                               : 'dev',
                                'crypto.key'                                      : '**************',
                                'anypoint.platform.client_id'                     : '**************',
                                'anypoint.platform.client_secret'                 : '**************',
                                'anypoint.platform.config.analytics.agent.enabled': true
                        ]
                ]))


    }
}
