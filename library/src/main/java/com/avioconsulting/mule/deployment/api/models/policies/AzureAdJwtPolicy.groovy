package com.avioconsulting.mule.deployment.api.models.policies

class AzureAdJwtPolicy extends JwtPolicy {
    AzureAdJwtPolicy(String azureAdTenantId,
                     String expectedAudience,
                     List<String> rolesAkaApiPermissionsToRequire = [],
                     Map<String, String> customClaimValidations = [:],
                     List<PolicyPathApplication> policyPathApplications = null,
                     boolean skipClientIdEnforcement = false,
                     Integer jwksCachingTtlInMinutes = null,
                     String version = null) {
        super("https://login.microsoftonline.com/${azureAdTenantId}/discovery/v2.0/keys",
              expectedAudience,
              "https://sts.windows.net/${azureAdTenantId}/",
              policyPathApplications,
              getCustomClaimValidations(rolesAkaApiPermissionsToRequire,
                                        customClaimValidations),
              // Azure AD calls it appid instead of client ID
              '#[vars.claimSet.appid]',
              skipClientIdEnforcement,
              jwksCachingTtlInMinutes,
              version)
    }

    private static Map<String, String> getCustomClaimValidations(List<String> rolesAkaApiPermissionsToRequire,
                                                                 Map<String, String> customClaimValidations) {
        def roleString = rolesAkaApiPermissionsToRequire.collect { roleName ->
            "(vars.claimSet.roles contains '${roleName}')"
        }.join(' or ')
        def roleClaims = roleString ? [
                roles: "#[${roleString}]".toString() // GString issues
        ] : [:]
        return roleClaims + customClaimValidations
    }
}
