package com.avioconsulting.mule.deployment.api.models

class Version extends com.fasterxml.jackson.core.Version {

    Version(int major, int minor, int patchLevel, String snapshotInfo, String groupId, String artifactId) {
        super(major, minor, patchLevel, snapshotInfo, groupId, artifactId)
    }

    String getQualifier() {
        return this._snapshotInfo
    }

    int compareTo(Version other) {
        int result = super.compareTo(other)

        // If no qualifiers are set, just use the super result
        if(this.getQualifier() == null && other.getQualifier() == null) {
            return result
        }

        if(result == 0) {
            if(this.getQualifier() != null && other.getQualifier() != null) {
                if(isInt(this.getQualifier()) && isInt(other.getQualifier())) {
                    // Both versions contain int qualifiers, compare numerically
                    return Integer.parseInt(this.getQualifier()) - Integer.parseInt(other.getQualifier())
                } else {
                    // At least one version qualifier is not an int, compare lexically
                    return this.getQualifier() <=> other.getQualifier()
                }
            } else {
                // Whichever qualifier is null is consider lower
                if (this.getQualifier() == null) {
                    return -1
                } else {
                    return 1
                }
            }
        }

        return result
    }

    private boolean  isInt(String intString) {
        try {
            Integer.parseInt(intString)
            return true
        } catch (Exception e) {
            return false
        }
    }
}
