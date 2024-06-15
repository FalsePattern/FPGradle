package com.falsepattern.examplemod;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

import java.lang.management.ManagementFactory;

@Mod(modid = "examplemod",
     name = "ExampleMod",
     version = Tags.VERSION)
public class MyMod {
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        System.out.println("Hello from Java " + ManagementFactory.getRuntimeMXBean().getVmVersion());
    }
}
