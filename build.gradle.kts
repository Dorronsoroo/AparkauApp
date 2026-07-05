// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    id("com.google.gms.google-services") version "4.5.0" apply false
    id("com.google.dagger.hilt.android") version "2.60" apply false

    id("org.sonarqube") version "7.3.1.8318"


}

sonarqube {
    properties {
        property("sonar.projectKey", "dorronsoroo_aparkauapp")
        property("sonar.organization", "dorronsoroo")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.coverage.jacoco.xmlReportPaths", "${project.rootDir}/app/build/reports/kover/report.xml")
    }
}
