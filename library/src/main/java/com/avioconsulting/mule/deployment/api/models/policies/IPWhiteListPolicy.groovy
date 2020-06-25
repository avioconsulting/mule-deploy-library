package com.avioconsulting.mule.deployment.api.models.policies

class IPWhiteListPolicy extends MulesoftPolicy {
    IPWhiteListPolicy(List<String> ipsToAllow,
                      String remoteIpExpression = "#[attributes.headers['x-forwarded-for']]",
                      String version = null,
                      List<PolicyPathApplication> policyPathApplications = null) {
        super('ip-whitelist',
              version ?: '1.2.2',
              getPolicyConfig(remoteIpExpression,
                              ipsToAllow),
              policyPathApplications)
    }

    private static Map<String, Object> getPolicyConfig(String remoteIpExpression,
                                                       List<String> ipsToAllow) {
        assert ipsToAllow.size() > 0: 'Need to white list something!'
        [
                ipExpression: remoteIpExpression,
                ips: ipsToAllow
        ]
    }
}
