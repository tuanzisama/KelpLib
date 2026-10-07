pluginManagement {
    repositories {
        // 首选国内可达镜像（本机国际直连不稳定）；Gradle 按序尝试直到解析成功。
        maven("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/central")
        // Gradle Plugin Portal（run-paper 等仅在可直接访问时启用）
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "KelpLib"

// 全部模块共享的依赖仓库（国际直连不稳定，国内镜像优先；Gradle 按序尝试）
dependencyResolutionManagement {
    repositories {
        maven("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") // Maven Central 镜像
        maven("https://maven.aliyun.com/repository/central")
        maven("https://repo.huaweicloud.com/repository/maven/")
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/") // Paper / Velocity API
        maven("https://repo.xenondevs.xyz/releases/") // InvUI
        maven("https://repo.panda-lang.org/releases/") // LiteCommands
    }
}

include("kelpLib-api")
include("kelpLib-core")
include("kelpLib-bukkit")
include("kelpLib-velocity")
