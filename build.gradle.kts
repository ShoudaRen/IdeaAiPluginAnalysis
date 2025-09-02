plugins {
  id("java")
  id("org.jetbrains.intellij") version "1.17.3"
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
  // 使用阿里云镜像加速依赖下载
  maven { url = uri("https://maven.aliyun.com/repository/public") }
  maven { url = uri("https://maven.aliyun.com/repository/central") }
  mavenCentral()
}

dependencies {
  // HTTP客户端
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  
  // JSON处理
  implementation("com.google.code.gson:gson:2.10.1")
  
  // 数据库连接
  implementation("mysql:mysql-connector-java:8.0.33")
  implementation("com.zaxxer:HikariCP:5.0.1")
  
  // 日志
  implementation("org.slf4j:slf4j-api:2.0.9")
  implementation("ch.qos.logback:logback-classic:1.4.11")
  
  // 工具类
  implementation("org.apache.commons:commons-lang3:3.13.0")
  implementation("commons-io:commons-io:2.11.0")
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
  version.set("2023.2.5")
  type.set("IC") // Target IDE Platform
  updateSinceUntilBuild.set(false)
  
  // 禁用代码插桩功能以避免JDK路径问题
  instrumentCode.set(false)
  
  plugins.set(listOf("com.intellij.java", "java"))
}

tasks {
  // Set the JVM compatibility versions
  withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:unchecked")
  }

  patchPluginXml {
    sinceBuild.set("232")
    untilBuild.set("242.*")
  }

  signPlugin {
    certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
    privateKey.set(System.getenv("PRIVATE_KEY"))
    password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
  }

  publishPlugin {
    token.set(System.getenv("PUBLISH_TOKEN"))
  }
}
