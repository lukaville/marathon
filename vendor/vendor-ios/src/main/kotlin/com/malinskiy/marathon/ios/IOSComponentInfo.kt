package com.malinskiy.marathon.ios

import com.malinskiy.marathon.execution.ComponentInfo
import java.io.File

data class IOSComponentInfo(
    val xctestrunPath: File,
    val derivedDataDir: File,
    val sourceRoot: File = File("."),
    override val name: String = "root"
) : ComponentInfo {

    val productsDir = derivedDataDir.resolve(PRODUCTS_PATH)

    private companion object {
        private const val PRODUCTS_PATH = "Build/Products"
    }
}
