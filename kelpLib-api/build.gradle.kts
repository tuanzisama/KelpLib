plugins {
    `java-library`
    `maven-publish`
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
    options.encoding = "UTF-8"
}

dependencies {
    // P1：api 模块仅依赖 Adventure 套件，禁止出现任何平台类型（Bukkit/Velocity）。
    api("net.kyori:adventure-api:4.17.0")
    api("net.kyori:adventure-text-minimessage:4.17.0")
    compileOnly("org.jetbrains:annotations:26.1.0")
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
    // CI/本地生成 JavaDoc 时不因 doclint 阻断（G5 的 JavaDoc 覆盖已人工保证）
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:all,-missing", "-quiet")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            // GitHub Packages 仅接受全小写 artifactId（混合大小写首次发布返回 422）
            artifactId = "kelp-lib-api"
            from(components["java"])
            pom {
                name.set("KelpLib API")
                description.set("Platform-agnostic stable API surface of KelpLib (utoverse server family)")
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
