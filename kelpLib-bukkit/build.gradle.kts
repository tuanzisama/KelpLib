plugins {
    `java-library`
    `maven-publish`
    id("com.gradleup.shadow")
    // 3.0.x 为与 Gradle 9.4.x 兼容的版本线（3.1.x 需要 gradle-plugin-api 9.7.0）
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

import org.gradle.api.attributes.java.TargetJvmVersion

java {
    // paper-api 26.3 的模块元数据要求 JVM 25 运行时（Paper 26.1+ 服务器同样要求 Java 25）；
    // options.release = 21 保持字节码与语言特性不超出 21（P5）。
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
    options.encoding = "UTF-8"
}

// options.release = 21 会让 TargetJvmVersion 解析属性取 21，而 paper-api 26.3 元数据声明
// 需要 JVM 25；部署目标本就是 Java 25 服务器，将该属性显式对齐到 25（字节码仍为 21 级）。
configurations.compileClasspath {
    attributes {
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
    }
}

// paper-api 已可解析（repo.papermc.io 经本地代理可达），编译期桩（src/stubs/java）停用；
// 版本与 runServer 冒烟运行的服务器保持一致（Paper 26.3-159）。
dependencies {
    api(project(":kelpLib-api"))
    implementation(project(":kelpLib-core"))

    compileOnly("io.papermc.paper:paper-api:26.3.build.159-beta")
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")
    compileOnly("org.yaml:snakeyaml:2.7")
    // 存储与 Redis 传输的运行时实现经 plugin.yml `libraries:` 动态引入（§8 内部型依赖）。
    compileOnly("com.zaxxer:HikariCP:6.3.3")
    compileOnly("io.lettuce:lettuce-core:6.8.2.RELEASE")
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
                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
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
