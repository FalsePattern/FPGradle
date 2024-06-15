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
        modid = "examplemod"
        name = "Example Mod"
//        version = "foobar"
        rootPkg = "$group.mymod"
    }

//    api {
//        packages = listOf("a", "b", "c")
//        packagesNoRecurse = listOf("d", "e")
//    }

//    mixin {
//        pkg = "mixins"
//        pluginClass = "MixinPlugin"
//        debug = false
//        hasMixinDeps = false
//    }

//    core {
//        coreModClass = "Core"
//        accessTransformerFile = "le_at.cfg"
//        containsMixinsAndOrCoreModOnly = false
//    }

//    shadow {
//        minimize = false
//        relocate = false
//    }

    tokens {
        tokenClass = "Tags"
//        modid = "MOD_ID"
//        name = "MOD_NAME"
//        version = "MOD_VERSION"
//        rootPkg = "ROOT_PKG"
    }

    //TODO: don't use inverted options!
    //TODO: how are the repo creds sourced?

    //TODO: publish jar?
    //TODO: publish dev?
    //TODO: publish src?

    //TODO: repoUrl, as string -or- uri setter?

//    publish {
//        noSources = false
//        repoUrl = uri("https://example.com/")
//        repoName = "example"
//        group = "com.myname"
//        artifact = "mymod-mc1.7.10"
//        version = "1.0.0"
//    }
}
