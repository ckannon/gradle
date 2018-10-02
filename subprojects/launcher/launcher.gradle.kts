import org.gradle.gradlebuild.testing.integrationtests.cleanup.WhenNotEmpty
import org.gradle.build.GradleStartScriptGenerator
import org.gradle.gradlebuild.test.integrationtests.IntegrationTest
import org.gradle.gradlebuild.unittestandcompile.ModuleType

plugins {
    id("gradlebuild.classycle")
}

configurations {
    register("startScriptGenerator")
}

dependencies {
    compile(project(":baseServices"))
    compile(project(":jvmServices"))
    compile(project(":core"))
    compile(project(":cli"))
    compile(project(":buildOption"))
    compile(project(":toolingApi"))
    compile(project(":native"))
    compile(project(":logging"))
    compile(project(":docs"))

    compile(library("asm"))
    compile(library("commons_io"))
    compile(library("commons_lang"))
    compile(library("slf4j_api"))

    integTestCompile(project(":internalIntegTesting"))
    integTestRuntime(project(":plugins"))
    integTestRuntime(project(":languageNative"))

    testFixturesApi(project(":internalIntegTesting"))
}
// Needed for testing debug command line option (JDWPUtil)
if (!rootProject.availableJavaInstallations.javaInstallationForTest.javaVersion.isJava9Compatible()) {
    dependencies {
        integTestRuntime(files(rootProject.availableJavaInstallations.javaInstallationForTest.toolsJar))
    }
}

if(rootProject.availableJavaInstallations.currentJavaInstallation.javaVersion.isJava8()) {
    // If running on Java 8 but compiling with Java 9, Groovy code would still be compiled by Java 8, so here we need the tools.jar
    dependencies {
        integTestCompileOnly(files(rootProject.availableJavaInstallations.currentJavaInstallation.toolsJar))
    }
}

gradlebuildJava {
    moduleType = ModuleType.ENTRY_POINT
}

testFixtures {
    from(":core")
    from(":languageJava")
    from(":messaging")
    from(":logging")
    from(":toolingApi")
}

val integTestTasks: DomainObjectCollection<IntegrationTest> by extra
integTestTasks.configureEach {
    maxParallelForks = Math.min(3, project.maxParallelForks)
}

val configureJar by tasks.registering {
    doLast {
        val classpath = listOf(":baseServices", ":coreApi", ":core").map { project(it).tasks.getByName<Jar>("jar").archivePath.name }.joinToString(" ")
        tasks.jar.get().manifest.attributes("Class-Path" to classpath)
    }
}

tasks.jar.configure {
    dependsOn(configureJar)
    manifest.attributes("Main-Class" to "org.gradle.launcher.GradleMain")
}

tasks.register<GradleStartScriptGenerator>("startScripts") {
    startScriptsDir = File("$buildDir/startScripts")
    launcherJar = tasks.jar.get().outputs.files
}

testFilesCleanup {
    policy.set(WhenNotEmpty.REPORT)
}
