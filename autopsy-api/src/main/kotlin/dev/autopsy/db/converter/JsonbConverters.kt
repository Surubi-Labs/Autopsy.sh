package dev.autopsy.db.converter

import org.postgresql.util.PGobject
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

@ReadingConverter
class JsonbReadingConverter : Converter<PGobject, String> {
    override fun convert(source: PGobject): String = source.value ?: ""
}

@WritingConverter
class JsonbWritingConverter : Converter<String, PGobject> {
    override fun convert(source: String): PGobject = PGobject().apply {
        type = "jsonb"
        value = source
    }
}
