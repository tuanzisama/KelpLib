plugins {
    `java-library`
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
    options.encoding = "UTF-8"
}

dependencies {
    api(project(":kelpLib-api"))

    // 平台双端运行时均提供或由平台 jar 捆绑，核心实现仅编译期需要（§4 依赖规则：不引入平台依赖）。
    compileOnly("net.kyori:adventure-api:4.17.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")
    compileOnly("org.jetbrains:annotations:26.1.0")

    // SnakeYAML：配置引擎基于节点树操作（保留注释与键顺序）；Paper 自带，Velocity 侧 relocate 捆绑。
    compileOnly("org.yaml:snakeyaml:2.7")
    // 存储：HikariCP 连接池（Bukkit 侧经 plugin.yml libraries 引入，Velocity 侧 relocate 捆绑）
    compileOnly("com.zaxxer:HikariCP:6.3.3")
    // Messenger Redis 传输：Lettuce（分发方式同上）
    compileOnly("io.lettuce:lettuce-core:6.8.2.RELEASE")
}
