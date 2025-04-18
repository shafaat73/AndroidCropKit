plugins {
  id("org.jetbrains.dokka")
  id("org.jetbrains.kotlin.android")
  id("com.android.library")
  id("org.jetbrains.kotlin.plugin.parcelize")
  id("com.vanniktech.maven.publish")
  id("app.cash.licensee")
  id("app.cash.paparazzi")
}

licensee {
  allow("Apache-2.0")
}

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
  }
}

android {
  namespace = "com.canhub.cropper"

  compileSdk = libs.versions.compileSdk.get().toInt()

  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()
  }

  buildFeatures {
    viewBinding = true
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  testOptions {
    unitTests {
      isIncludeAndroidResources = true
    }
  }
}

dependencies {
  implementation(libs.androidx.activity.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.exifinterface)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
}

dependencies {
  testImplementation(libs.androidx.fragment.testing)
  testImplementation(libs.androidx.test.junit)
  testImplementation(libs.junit)
  testImplementation(libs.mock)
  testImplementation(libs.robolectric)
}

// Workaround https://github.com/cashapp/paparazzi/issues/1231
plugins.withId("app.cash.paparazzi") {
  // Defer until afterEvaluate so that testImplementation is created by Android plugin.
  afterEvaluate {
    dependencies.constraints {
      add("testImplementation", "com.google.guava:guava") {
        attributes {
          attribute(
            TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
            objects.named(TargetJvmEnvironment::class.java, TargetJvmEnvironment.STANDARD_JVM),
          )
        }
        because(
          "LayoutLib and sdk-common depend on Guava's -jre published variant." +
            "See https://github.com/cashapp/paparazzi/issues/906.",
        )
      }
    }
  }
}


afterEvaluate {
  android.libraryVariants.forEach { variant ->
    publishing.publications.create(variant.name, MavenPublication::class.java) {
      from(components.findByName(variant.name))

      groupId = project.property("GROUP").toString()
      artifactId = project.property("POM_ARTIFACT_ID").toString()
      version = project.property("VERSION_NAME").toString()
    }
  }
}
