package com.avioconsulting.mule.deployment.api.models

import org.junit.jupiter.api.Test
import static org.junit.Assert.*

class VersionTest {

    @Test
    void compareTo_noQualifier() {
        Version one = new Version(1,0,0,null,null,null)
        Version anotherOne = new Version(1,0,0,null,null,null)
        Version two = new Version(2,0,0,null,null,null)
        assertTrue one < two
        assertTrue two > one
        assertTrue one == anotherOne
    }

    @Test
    void compareTo_withQualifierInt() {
        Version one = new Version(1,0,0,"1",null,null)
        Version anotherOne = new Version(1,0,0,"1",null,null)
        Version onePlusTwo = new Version(1,0,0,"2",null,null)
        Version two = new Version(2,0,0,"0",null,null)
        assertTrue one < two
        assertTrue two > one
        assertTrue one == anotherOne
        assertTrue one < onePlusTwo
        assertTrue onePlusTwo > one
    }

    @Test
    void compareTo_withQualifierString() {
        Version one = new Version(1,0,0,"one",null,null)
        Version anotherOne = new Version(1,0,0,"one",null,null)
        Version onePlusTwo = new Version(1,0,0,"two",null,null)
        Version onePlusTwoInt = new Version(1,0,0,"2",null,null)
        Version two = new Version(2,0,0,"0",null,null)
        assertTrue one < two
        assertTrue two > one
        assertTrue one == anotherOne
        assertTrue one < onePlusTwo
        assertTrue onePlusTwo > one
        // Since only one is an integer string comparison is still used, so 2 < one
        assertTrue one > onePlusTwoInt
    }

    @Test
    void compareTo_withQualifierNotMatched() {
        Version one = new Version(1,0,0,null,null,null)
        Version onePlusTwo = new Version(1,0,0,"2", null,null)
        Version two = new Version(2,0,0,"0",null,null)
        assertTrue one < two
        assertTrue two > one
        assertTrue one < onePlusTwo
        assertTrue onePlusTwo > one
    }
}
