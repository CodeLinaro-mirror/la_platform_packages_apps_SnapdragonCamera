package c2pa_config

import (
        "android/soong/android"
        "android/soong/cc"
)

func init() {
    // for DEBUG
    android.RegisterModuleType("c2pa_config", c2pa_configDefaultsFactory)
}

func c2pa_configDefaultsFactory() (android.Module) {
    module := cc.DefaultsFactory()
    android.AddLoadHook(module, c2pa_configDefaults)
    return module
}

func c2pa_configDefaults(ctx android.LoadHookContext) {
    type props struct {
        Cflags []string
        Shared_libs []string
    }

    p := &props{}
    target_variant := ctx.AConfig().Getenv("TARGET_BOARD_PLATFORM")

    switch target_variant {
        case "sun":
            p.Cflags = append(p.Cflags, "-DENABLE_C2PA_LIB")
            p.Shared_libs = append(p.Shared_libs, "vendor.qti.hardware.c2pa-V1-ndk")
            ctx.AppendProperties(p)
        default:
            //do nothing
    }
}