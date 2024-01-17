package com.avioconsulting.mule.deployment.api.models

// vCores size.
// Reference: https://docs.mulesoft.com/cloudhub-2/ch2-architecture#cloudhub-2-replicas
enum VCoresSize {
    vCore1GB(0.1),
    vCore2GB(0.2),
    vCore3GB(0.5),
    vCore4GB(1),
    vCore6GB(1.5),
    vCore8GB(2),
    vCore9GB(2.5),
    vCore11GB(3),
    vCore13GB(3.5),
    vCore15GB(4)

    Double vCoresSize

    VCoresSize(Double vCoresSize) {
        this.vCoresSize = vCoresSize
    }
}
