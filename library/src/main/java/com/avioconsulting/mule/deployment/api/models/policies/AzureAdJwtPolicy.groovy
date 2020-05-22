package com.avioconsulting.mule.deployment.api.models.policies

class AzureAdJwtPolicy extends JwtPolicy {
    AzureAdJwtPolicy(String azureAdTenantId,
                     String expectedAudience,
                     List<String> rolesAkaApiPermissionsToRequire = [],
                     List<PolicyPathApplication> policyPathApplications = null,
                     boolean skipClientIdEnforcement = false,
                     Integer jwksCachingTtlInMinutes = null,
                     String version = null) {
        super("https://login.microsoftonline.com/${azureAdTenantId}/discovery/v2.0/keys",
              expectedAudience,
              "https://sts.windows.net/${azureAdTenantId}/",
              policyPathApplications,
              [:],
              // Azure AD calls it appid instead of client ID
              '#[vars.claimSet.appid]',
              skipClientIdEnforcement,
              jwksCachingTtlInMinutes,
              version)
    }
}
