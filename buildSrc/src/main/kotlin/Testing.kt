import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.junit.platform.gradle.plugin.EnginesExtension
import org.junit.platform.gradle.plugin.FiltersExtension
import org.junit.platform.gradle.plugin.JUnitPlatformExtension

object Testing {
    fun configure(project: Project) {
        project.apply(plugin = "org.junit.platform.gradle.plugin")

        project.dependencies {
            add("testImplementation", TestLibraries.kluent)
            add("testImplementation", TestLibraries.mockitoKotlin)
            add("testImplementation", TestLibraries.spekAPI)
            add("testImplementation", TestLibraries.junit5)
            add("testRuntime", TestLibraries.spekJUnitPlatformEngine)
            add("testRuntime", TestLibraries.jupiterEngine)
        }

        project.extensions.getByType(JUnitPlatformExtension::class.java).apply {
            filters {
                engines {
                    include("spek")
                }
            }
            enableStandardTestTask = true
        }

        project.tasks.withType<Test>().all {
            project.tasks.getByName("check").dependsOn(this)
            useJUnitPlatform()
        }

        project.tasks.getByName("junitPlatformTest").outputs.upToDateWhen { false }
        project.tasks.getByName("test").outputs.upToDateWhen { false }
    }

    private fun JUnitPlatformExtension.filters(setup: FiltersExtension.() -> Unit) {
        when (this) {
            is ExtensionAware -> extensions.getByType(FiltersExtension::class.java).setup()
            else -> throw IllegalArgumentException("${this::class} must be an instance of ExtensionAware")
        }
    }

    private fun FiltersExtension.engines(setup: EnginesExtension.() -> Unit) {
        when (this) {
            is ExtensionAware -> extensions.getByType(EnginesExtension::class.java).setup()
            else -> throw IllegalArgumentException("${this::class} must be an instance of ExtensionAware")
        }
    }
}
