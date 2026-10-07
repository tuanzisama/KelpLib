plugins {
    `java-library`
    `maven-publish`
    id("com.gradleup.shadow")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
    options.encoding = "UTF-8"
}

// 编译期桩：本构建环境无法访问 velocity-api 仓库时，用本地桩镜像所需 API 子集。
// stubs 仅参与编译（不进入 jar 产物）；velocity-plugin.json 以资源文件手工提供。
sourceSets.create("stubs")

repositories {
    maven("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
    maven("https://maven.aliyun.com/repository/central")
}

dependencies {
    api(project(":kelpLib-api"))
    implementation(project(":kelpLib-core"))

    compileOnly(sourceSets["stubs"].output)
    // compileOnly("com.velocitypowered:velocity-api:3.5.1")
    // annotationProcessor("com.velocitypowered:velocity-api:3.5.1")
    "stubsImplementation"("net.kyori:adventure-api:4.17.0")

    // 导出型依赖（无，LiteCommands 不可达时以自研命令层回退，见 §11.4 回退路径）

    // Adventure MiniMessage：非 relocate 捆绑兜底（代理端若未内置则由本插件补齐）
    implementation("net.kyori:adventure-text-minimessage:4.17.0")
    implementation("org.slf4j:slf4j-api:2.0.17")

    // 内部型依赖：Velocity 无 libraries 机制，shadow + relocate 捆绑（§8）
    implementation("org.yaml:snakeyaml:2.7")
    implementation("com.zaxxer:HikariCP:6.3.3") { exclude(group = "org.slf4j") }
    implementation("io.lettuce:lettuce-core:6.8.2.RELEASE") {
        exclude(group = "io.netty")     // 使用代理端自带 Netty
        exclude(group = "org.slf4j")
    }
    implementation("com.h2database:h2:2.5.252")
    implementation("org.xerial:sqlite-jdbc:3.53.4.0")
    implementation("com.mysql:mysql-connector-j:26.7.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.10")
}

tasks.processResources {
    val props = mapOf("version" to version)
    filesMatching("velocity-plugin.json") {
        expand(props)
    }
}

tasks.shadowJar {
    archiveClassifier = "all"
    listOf(
        "com.zaxxer.hikari" to "hikari",
        "io.lettuce" to "lettuce",
        "io.projectreactor" to "reactor",
        "org.reactivestreams" to "rstreams",
        "com.mysql" to "mysql",
        "org.mariadb.jdbc" to "mariadb",
        "org.h2" to "h2",
        "org.sqlite" to "sqlite",
        "org.yaml.snakeyaml" to "snakeyaml",
        "com.google.protobuf" to "protobuf"
    ).forEach { (pkg, name) ->
        relocate(pkg, "ink.tuanzi.kelpLib.libs.$name")
    }
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            // plain jar = 编译依赖；shadow -all jar 已由 shadow 插件自动挂入 java 组件（all 分类器）
            from(components["java"])
            pom {
                name.set("KelpLib Velocity")
                description.set("Velocity platform module of KelpLib (plugin jar under 'all' classifier)")
                url.set("https://github.com/Utoverse/KelpLib")
                developers {
                    developer {
                        id.set("evenwan")
                        name.set("evenwan")
                    }
                }
            }
        }
    }
    repositories {
        // GitHub Packages（仅 CI 中注册；本地 publishToMavenLocal 不受影响）
        if (System.getenv("GITHUB_TOKEN") != null) {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/" + (System.getenv("GITHUB_REPOSITORY") ?: "utoverse/KelpLib"))
                credentials {
                    username = System.getenv("GITHUB_ACTOR") ?: "github-actions"
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
}
