package dev.autopsy.config

import dev.autopsy.db.converter.JsonbReadingConverter
import dev.autopsy.db.converter.JsonbWritingConverter
import org.springframework.context.annotation.Configuration
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration

@Configuration
class JdbcConfig : AbstractJdbcConfiguration() {

    override fun userConverters(): List<Any> = listOf(
        JsonbReadingConverter(),
        JsonbWritingConverter(),
    )
}
