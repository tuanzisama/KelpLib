plugins {
    `java-library`
    `maven-publish`
    id("com.gradleup.shadow")
    // 3.0.x 为与 Gradle 9.4.x 兼容的版本线（3.1.x 需要 gradle-plugin-api 9.7.0）
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
    options.encoding = "UTF-8"
}

// 编译期桩：本构建环境无法访问 paper-api 仓库时，用本地桩镜像所需 API 子集。
// stubs 仅参与编译（不进入 jar 产物）；若环境可解析 paper-api，将下方 paper-api 依赖启用
// 并删除 sourceSets.create("stubs") 与 compileOnly(sourceSets["stubs"].output) 两行即可。
sourceSets.create("stubs")

dependencies {
    api(project(":kelpLib-api"))
    implementation(project(":kelpLib-core"))

    // compileOnly("io.papermc.paper:paper-api:26.3.build.+")
    compileOnly(sourceSets["stubs"].output)
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")
    compileOnly("org.yaml:snakeyaml:2.7")
    // 存储与 Redis 传输的运行时实现经 plugin.yml `libraries:` 动态引入（§8 内部型依赖）。
    compileOnly("com.zaxxer:HikariCP:6.3.3")
    compileOnly("io.lettuce:lettuce-core:6.8.2.RELEASE")

    // 桩源集自身的编译依赖（paper-api 同时携带 Adventure 类型）
    "stubsImplementation"("net.kyori:adventure-api:4.17.0")
}

tasks.processResources {
    val props = mapOf("version" to version)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.shadowJar {
    archiveClassifier = "all"
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            // GitHub Packages 仅接受全小写 artifactId（混合大小写首次发布返回 422）
            artifactId = "kelp-lib-bukkit"
            // plain jar = 编译依赖；shadow -all jar 已由 shadow 插件自动挂入 java 组件（all 分类器）
            from(components["java"])
            pom {
                name.set("KelpLib Bukkit/Folia")
                description.set("Paper/Folia platform module of KelpLib (plugin jar under 'all' classifier)")
                url.set("https://github.com/tuanzisama/KelpLib")
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
                url = uri("https://maven.pkg.github.com/" + (System.getenv("GITHUB_REPOSITORY") ?: "tuanzisama/KelpLib"))
                credentials {
                    username = System.getenv("GITHUB_ACTOR") ?: "github-actions"
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
}

tasks {
    runServer {
        minecraftVersion("26.3")
        jvmArgs("-Xms2G", "-Xmx2G")
    }
}
