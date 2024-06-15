plugins {
    id("fpgradle-minecraft")
}

group = "com.falsepattern"
MC {
    modName = "ExampleMod"
    modId = "examplemod"
    modGroup = "com.falsepattern.examplemod"
    generateGradleTokenClass = "Tags"
}

