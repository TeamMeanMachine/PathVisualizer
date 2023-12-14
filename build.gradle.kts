import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    application
    kotlin("jvm") version "1.7.20"
    id("com.github.gmazzo.buildconfig") version "3.0.0"
    id("org.openjfx.javafxplugin") version "0.0.13"
}

buildConfig {
    buildConfigField ("long", "BUILD_TIME", "${System.currentTimeMillis()}L")
}

val wpiLibVersion = "2023.1.1"
repositories {
    mavenCentral()
    maven { setUrl("https://frcmaven.wpi.edu/artifactory/release/")}
    maven { setUrl("https://plugins.gradle.org/m2/")}
    maven { setUrl("https://maven.ctr-electronics.com/release/") }
    maven { setUrl("https://maven.revrobotics.com/") }
}

javafx {
    version = "19"
    modules = "javafx.controls,javafx.fxml".split(",").toMutableList()
}
application {
    mainClass.set("org.team2471.frc.pathvisualizer.PathVisualizer")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.20")
    implementation("edu.wpi.first.ntcore:ntcore-java:$wpiLibVersion")
    implementation("edu.wpi.first.ntcore:ntcore-jni:$wpiLibVersion:windowsx86-64")
    implementation("edu.wpi.first.ntcore:ntcore-jni:$wpiLibVersion:osxuniversal")
    implementation("edu.wpi.first.wpiutil:wpiutil-java:$wpiLibVersion")
    implementation("edu.wpi.first.wpiutil:wpiutil-jni:$wpiLibVersion:windowsx86-64")
    implementation("edu.wpi.first.wpiutil:wpiutil-jni:$wpiLibVersion:osxuniversal")
    implementation("edu.wpi.first.wpimath:wpimath-java:$wpiLibVersion")
    implementation("edu.wpi.first.wpilibj:wpilibj-java:$wpiLibVersion")
    implementation("org.team2471.lib:meanlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
    implementation("com.github.gmazzo.buildconfig:com.github.gmazzo.buildconfig.gradle.plugin:3.0.0")
    implementation("com.google.code.gson:gson:2.8.9")
}
java {
    withSourcesJar()
    withJavadocJar()
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_16.toString()
}

