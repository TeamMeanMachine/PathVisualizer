import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    application
    kotlin("jvm") version "1.9.22"
    id("com.github.gmazzo.buildconfig") version "3.0.0"
    id("org.openjfx.javafxplugin") version "0.0.13"
}

buildConfig {
    buildConfigField ("long", "BUILD_TIME", "${System.currentTimeMillis()}L")
}

val wpiLibVersion = "2024.2.1"
repositories {
    mavenCentral()
    maven { setUrl("https://frcmaven.wpi.edu/artifactory/release/")}
    maven { setUrl("https://plugins.gradle.org/m2/")}
    maven { setUrl("https://maven.ctr-electronics.com/release/") }
    maven { setUrl("https://maven.revrobotics.com/") }
    maven { setUrl("https://maven.photonvision.org/repository/internal")}
    maven { setUrl("https://maven.photonvision.org/repository/snapshots")}
}

javafx {
    version = "19"
    modules = "javafx.controls,javafx.fxml".split(",").toMutableList()
}
application {
    mainClass.set("org.team2471.frc.pathvisualizer.PathVisualizer")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
    implementation("edu.wpi.first.apriltag:apriltag-java:$wpiLibVersion")
    implementation("edu.wpi.first.ntcore:ntcore-java:$wpiLibVersion")
    implementation("edu.wpi.first.ntcore:ntcore-jni:$wpiLibVersion:windowsx86-64")
    implementation("edu.wpi.first.wpinet:wpinet-java:$wpiLibVersion")
    implementation("edu.wpi.first.wpinet:wpinet-jni:$wpiLibVersion:windowsx86-64")
    implementation("edu.wpi.first.wpiutil:wpiutil-java:$wpiLibVersion")
    implementation("edu.wpi.first.wpiutil:wpiutil-jni:$wpiLibVersion:windowsx86-64")
    implementation("edu.wpi.first.wpiunits:wpiunits-java:$wpiLibVersion")
    implementation("edu.wpi.first.wpimath:wpimath-java:$wpiLibVersion")
    implementation("edu.wpi.first.wpilibj:wpilibj-java:$wpiLibVersion")
    implementation("edu.wpi.first.hal:hal-jni:$wpiLibVersion:windowsx86-64")
    implementation("org.team2471.lib:meanlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
    implementation("com.github.gmazzo.buildconfig:com.github.gmazzo.buildconfig.gradle.plugin:3.0.0")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.1")
    implementation("org.photonvision:photonlib-java:v2024.2.4")
    implementation("org.photonvision:photontargeting-java:v2024.2.4")
    implementation("us.hebi.quickbuf:quickbuf-runtime:1.4")
    implementation("org.ejml:ejml-all:0.43.1")
}
java {
    withSourcesJar()
    withJavadocJar()
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
}

