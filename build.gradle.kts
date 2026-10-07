plugins {
    // 由各平台模块按需应用（kelpLib-bukkit / kelpLib-velocity 的捆绑打包）
    // 9.5.1 为与 Gradle 9.4.x 兼容的最新版（9.6.x 需要 gradle-plugin-api 9.7.0）
    id("com.gradleup.shadow") version "9.5.1" apply false
}

allprojects {
    group = "ink.tuanzi"
    description = "KelpLib — shared utility & extension API library for the utoverse server family"
}
