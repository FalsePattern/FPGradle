plugins {
    id("fpgradle-minecraft")
}

group = "com.example"

minecraft_fp {
    java {
        compatibility = modern
//        version = JavaVersion.VERSION_21
    }

    mod {
        name = "Example Mod"
        id = "examplemod"
        group = "com.example.mymod"
//        version = "foobar"
    }
//
//    api {
//        packages = listOf("a", "b", "c")
//        packagesNoRecurse = listOf("d", "e")
//    }
//
//    mixin {
//        debug = false
//        forceEnable = false
//        plugin = "MixinPlugin"
//        pkg = "mixins"
//    }
//
//    core {
//        coreModClass = "Core"
//        accessTransformerFile = "le_at.cfg"
//        containsMixinsAndOrCoreModOnly = false
//    }
//
//    shadow {
//        minimize = false
//        relocate = false
//    }

//    token {
//        tokenClass = "Tags"
//        modId = "MODID"
//        modName = "MODNAME"
//        version = "VERSION"
//        groupName = "GROUPNAME"
//    }

//    publish {
//        noSources = false
//        repoUrl = uri("https://example.com/")
//        repoName = "example"
//        group = "com.myname"
//        artifact = "mymod-mc1.7.10"
//        version = "1.0.0"
//    }
}