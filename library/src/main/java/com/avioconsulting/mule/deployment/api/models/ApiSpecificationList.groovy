package com.avioconsulting.mule.deployment.api.models

class ApiSpecificationList extends ArrayList<ApiSpecification> {
    ApiSpecificationList(List<ApiSpecification> candidates) {
        if (candidates.size() > 1) {
            def projectNames = candidates.collect { s -> s.name }
            def branchNames = candidates.collect { s -> s.designCenterBranchName }
            if (projectNames.size() != projectNames.unique().size() && branchNames.size() != branchNames.unique().size()) {
                throw new Exception('You either need separate design center project names for v1 and v2 OR you need different designCenterBranchNames')
            }
            def autoDiscNames = candidates.collect { s -> s.autoDiscoveryPropertyName }
            if (autoDiscNames.size() != autoDiscNames.unique().size()) {
                throw new Exception("If you have multiple API specs, you must specify a unique `autoDiscoveryPropertyName` for all of them!")
            }
        }
        super.addAll(candidates)
    }
}
