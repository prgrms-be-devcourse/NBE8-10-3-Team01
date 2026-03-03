package com.plog.testUtil

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.plog.global.exception.errorCode.ErrorCode
import com.plog.global.response.Response
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.core.IsNull
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * MockMvc 테스트에서 JSON 응답의 필드 구조와 값을 검증하기 위한
 * 커스텀 [ResultMatcher] 구현체입니다.
 */
class JsonFieldMatcher(private val matchers: List<ResultMatcher>) : ResultMatcher {

    override fun match(result: MvcResult) {
        for (matcher in matchers) {
            matcher.match(result)
        }
    }

    companion object {
        @JvmStatic
        fun hasStatus(errorCode: ErrorCode): ResultMatcher {
            return ResultMatcher { result ->
                val status = errorCode.httpStatus
                val actualStatus = result.response.status
                assertThat(actualStatus)
                    .withFailMessage(
                        "기대한 Http status 는 %d 였지만, 실제는 %d 였습니다.",
                        status.value(), actualStatus
                    )
                    .isEqualTo(status.value())
            }
        }

        @JvmStatic
        fun hasKey(key: String, value: String): ResultMatcher {
            val matchers = listOf(jsonPath("$.$key").value(value))
            return JsonFieldMatcher(matchers)
        }

        @JvmStatic
        fun hasKey(errorCode: ErrorCode): ResultMatcher {
            val matchers = listOf(
                jsonPath("$.status").value("fail"),
                jsonPath("$.message").value(errorCode.message)
            )
            return JsonFieldMatcher(matchers)
        }

        @JvmStatic
        fun hasKey(message: String): ResultMatcher {
            val matchers = listOf(
                jsonPath("$.status").value("fail"),
                jsonPath("$.message").value(message)
            )
            return JsonFieldMatcher(matchers)
        }

        @JvmStatic
        fun <T> hasKey(response: Response<T>): ResultMatcher {
            val matchers = mutableListOf<ResultMatcher>()

            matchers.add(jsonPath("$.status").value(response.status))

            response.message?.let {
                matchers.add(jsonPath("$.message").value(it))
            }

            val data = response.data
            if (data == null) {
                matchers.add(jsonPath("$.data").value(IsNull.nullValue()))
            } else {
                matchers.addAll(buildMatchersForValue("data", data))
            }

            return JsonFieldMatcher(matchers)
        }

        private fun matcherForList(root: String, values: List<*>): List<ResultMatcher> {
            val matchers = mutableListOf<ResultMatcher>()
            matchers.add(jsonPath("$.$root.length()").value(values.size))

            if (values.size == 1) {
                val value = values[0]
                val elementPath = "$root[0]"
                if (value != null) {
                    matchers.addAll(buildMatchersForValue(elementPath, value))
                }
            }
            return matchers
        }

        private fun buildMatchersForValue(path: String, value: Any?): List<ResultMatcher> {
            val matchers = mutableListOf<ResultMatcher>()

            when {
                value == null -> {
                    matchers.add(jsonPath("$.$path").value(null as Any?))
                }
                isSimpleValue(value) -> {
                    matchers.add(jsonPath("$.$path").value(value))
                }
                value is LocalDate -> {
                    matchers.add(
                        jsonPath("$.$path").value(value.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    )
                }
                value is LocalTime -> {
                    matchers.add(
                        jsonPath("$.$path").value(value.format(DateTimeFormatter.ISO_LOCAL_TIME))
                    )
                }
                value is LocalDateTime -> {
                    val formatted = value.truncatedTo(ChronoUnit.SECONDS)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                    matchers.add(jsonPath("$.$path").value(formatted))
                }
                value is List<*> -> {
                    matchers.addAll(matcherForList(path, value))
                }
                else -> {
                    val mapper = createMapper()
                    val nestedMap = mapper.convertValue(value, Map::class.java) as Map<String, Any?>
                    for ((key, entryValue) in nestedMap) {
                        matchers.addAll(buildMatchersForValue("$path.$key", entryValue))
                    }
                }
            }
            return matchers
        }

        private fun isSimpleValue(value: Any): Boolean {
            return value is String || value is Number || value is Boolean
        }

        private fun createMapper(): ObjectMapper {
            return ObjectMapper().apply {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
        }
    }
}
