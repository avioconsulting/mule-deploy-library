package com.avioconsulting.mule.deployment.api.models.policies

/***
 * Access through the DLB and a client WAF outside the DLB results in a chain of addresses like this:
 *
 * 172.0.0.1, 192.168.1.1, 10.0.0.1
 *
 * In this scenario, assuming the client tries to be malicious and supply their own X-Forwarded-For header of 172.0.0.1
 * 10.0.0.1 is the immediate endpoint before the DLB (like a WAF)
 * 192.168.1.1 is the actual client
 * 172.0.0.1 is the client's "malicious" IP.
 *
 * This model will tell the policy to only look at the last hop
 */
class DLBIPWhiteListPolicy extends IPWhiteListPolicy {
    /**
     * Construct one
     * @param ipsToAllow IPs to allow
     * @param dwListIndexToUse. Which IP in the X-Forwarded-For list to use. By default, it's -2, which means 2nd from the last address. If you're using a WAF in front of the DLB, this is a good default. If people directly hit the DLB, then you might want to use -1 for the last address.
     * @param version
     * @param policyPathApplications
     */
    DLBIPWhiteListPolicy(List<String> ipsToAllow,
                         Integer dwListIndexToUse = null,
                         String version = null,
                         List<PolicyPathApplication> policyPathApplications = null) {
        super(ipsToAllow,
              "#[do {    var ipList = attributes.headers['x-forwarded-for'] splitBy ',' map trim(\$)    ---   if (sizeOf(ipList) == 1) ipList[0] else ipList[${dwListIndexToUse ?: -2}] }]",
              version,
              policyPathApplications)
    }
}
