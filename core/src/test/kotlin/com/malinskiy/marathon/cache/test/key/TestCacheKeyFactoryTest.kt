package com.malinskiy.marathon.cache.test.key

import com.malinskiy.marathon.cache.CacheKey
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.execution.ComponentInfo
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.test.TestComponentInfo
import com.malinskiy.marathon.test.factory.configuration
import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotEqual
import org.junit.jupiter.api.Test
import com.malinskiy.marathon.test.Test as MarathonTest

class TestCacheKeyFactoryTest {

    @Test
    fun differentCacheKeysForDifferentMarathonVersion() {
        runBlocking {
            val firstKey = createCacheKey(marathonVersion = "1.0")
            val secondKey = createCacheKey(marathonVersion = "1.1")

            firstKey shouldNotEqual secondKey
        }
    }

    @Test
    fun sameCacheKeysForSameMarathonVersion() {
        runBlocking {
            val firstKey = createCacheKey(marathonVersion = "1.0")
            val secondKey = createCacheKey(marathonVersion = "1.0")

            firstKey shouldEqual secondKey
        }
    }

    @Test
    fun differCacheKeysForDifferentComponentCacheKeys() {
        runBlocking {
            val firstKey = createCacheKey(componentCacheKey = "abc")
            val secondKey = createCacheKey(componentCacheKey = "def")

            firstKey shouldNotEqual secondKey
        }
    }

    @Test
    fun sameCacheKeysForSameComponentCacheKeys() {
        runBlocking {
            val firstKey = createCacheKey(componentCacheKey = "abc")
            val secondKey = createCacheKey(componentCacheKey = "abc")

            firstKey shouldEqual secondKey
        }
    }

    @Test
    fun differentCacheKeysForDifferentDevicePools() {
        runBlocking {
            val firstKey = createCacheKey(devicePoolId = DevicePoolId("abc"))
            val secondKey = createCacheKey(devicePoolId = DevicePoolId("def"))

            firstKey shouldNotEqual secondKey
        }
    }

    @Test
    fun sameCacheKeysForTheSameDevicePools() {
        runBlocking {
            val firstKey = createCacheKey(devicePoolId = DevicePoolId("abc"))
            val secondKey = createCacheKey(devicePoolId = DevicePoolId("abc"))

            firstKey shouldEqual secondKey
        }
    }

    @Test
    fun differentCacheKeysForDifferentCodeCoverageConfigurations() {
        runBlocking {
            val firstKey = createCacheKey(configuration = createConfiguration(codeCoverageEnabled = true))
            val secondKey = createCacheKey(configuration = createConfiguration(codeCoverageEnabled = false))

            firstKey shouldNotEqual secondKey
        }
    }

    @Test
    fun sameCacheKeysForSameCodeCoverageConfigurations() {
        runBlocking {
            val firstKey = createCacheKey(configuration = createConfiguration(codeCoverageEnabled = true))
            val secondKey = createCacheKey(configuration = createConfiguration(codeCoverageEnabled = true))

            firstKey shouldEqual secondKey
        }
    }

    @Test
    fun differentCacheKeysForDifferentTestPackageNames() {
        runBlocking {
            val firstKey = createCacheKey(test = createTest(packageName = "abc"))
            val secondKey = createCacheKey(test = createTest(packageName = "def"))

            firstKey shouldNotEqual secondKey
        }
    }

    @Test
    fun sameCacheKeysForTheSamePackageNames() {
        runBlocking {
            val firstKey = createCacheKey(test = createTest(packageName = "abc"))
            val secondKey = createCacheKey(test = createTest(packageName = "abc"))

            firstKey shouldEqual secondKey
        }
    }

    @Test
    fun differentCacheKeysForDifferentClassNames() {
        runBlocking {
            val firstKey = createCacheKey(test = createTest(clazz = "abc"))
            val secondKey = createCacheKey(test = createTest(clazz = "def"))

            firstKey shouldNotEqual secondKey
        }
    }

    @Test
    fun sameCacheKeysForTheSameClassNames() {
        runBlocking {
            val firstKey = createCacheKey(test = createTest(clazz = "abc"))
            val secondKey = createCacheKey(test = createTest(clazz = "abc"))

            firstKey shouldEqual secondKey
        }
    }

    @Test
    fun differentCacheKeysForDifferentMethodNames() {
        runBlocking {
            val firstKey = createCacheKey(test = createTest(method = "abc"))
            val secondKey = createCacheKey(test = createTest(method = "def"))

            firstKey shouldNotEqual secondKey
        }
    }

    @Test
    fun sameCacheKeysForTheSameMethodNames() {
        runBlocking {
            val firstKey = createCacheKey(test = createTest(method = "abc"))
            val secondKey = createCacheKey(test = createTest(method = "abc"))

            firstKey shouldEqual secondKey
        }
    }
}

private fun createCacheKey(
    marathonVersion: String = "123",
    componentCacheKey: String = "abc",
    configuration: Configuration = createConfiguration(),
    devicePoolId: DevicePoolId = DevicePoolId("omni"),
    test: MarathonTest = createTest()
): CacheKey = runBlocking {
    val componentCacheKeyProvider = object : ComponentCacheKeyProvider {
        override suspend fun getCacheKey(componentInfo: ComponentInfo): String = componentCacheKey
    }
    val versionNameProvider = mock<VersionNameProvider> {
        on { this.versionName }.thenReturn(marathonVersion)
    }
    val cacheKeyFactory = TestCacheKeyFactory(componentCacheKeyProvider, versionNameProvider, configuration)
    cacheKeyFactory.getCacheKey(devicePoolId, test)
}

private fun createTest(
    packageName: String = "com.test",
    clazz: String = "Test",
    method: String = "test1"
) = MarathonTest(
    pkg = packageName,
    clazz = clazz,
    method = method,
    componentInfo = TestComponentInfo(someInfo = "someInfo", name = "component-name"),
    metaProperties = emptyList()
)

private fun createConfiguration(codeCoverageEnabled: Boolean = false) = configuration {
    isCodeCoverageEnabled = codeCoverageEnabled
}
