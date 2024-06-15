package com.example.mymod;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = Tags.MOD_ID,
     name = Tags.MOD_NAME,
     version = Tags.MOD_VERSION)
public class MyMod {
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        switch (1 + 1) {
            case 1 -> {}
            case 2 -> {
                System.out.println("Hello from Java " + System.getProperty("java.version"));
            }
        }
    }
}
