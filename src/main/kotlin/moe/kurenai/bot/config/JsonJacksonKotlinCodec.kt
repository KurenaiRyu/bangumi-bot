package moe.kurenai.bot.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.redisson.codec.JsonJacksonCodec

class JsonJacksonKotlinCodec(mapper: ObjectMapper) : JsonJacksonCodec(mapper) {

    override fun initTypeInclusion(mapObjectMapper: ObjectMapper?) {
    }
}