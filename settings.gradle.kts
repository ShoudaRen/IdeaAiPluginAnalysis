pluginManagement {
    repositories {
        // 使用阿里云镜像加速插件下载
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "demo"