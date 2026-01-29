package com.example.notification

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.IsolationMode

/**
 * Kotest project configuration
 */
class ProjectConfig : AbstractProjectConfig() {
    override val isolationMode = IsolationMode.InstancePerLeaf
    override val parallelism = 4
}
