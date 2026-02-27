package com.plog.domain.member.util

import org.springframework.stereotype.Component
import java.util.*


/** TODO: 주석 채우기
 * 코드에 대한 전체적인 역할을 적습니다.
 * <p>
 * 코드에 대한 작동 원리 등을 적습니다.
 *
 * <p><b>상속 정보:</b><br>
 * 상속 정보를 적습니다.
 *
 * <p><b>주요 생성자:</b><br>
 * {@code ExampleClass(String example)}  <br>
 * 주요 생성자와 그 매개변수에 대한 설명을 적습니다. <br>
 *
 * <p><b>빈 관리:</b><br>
 * 필요 시 빈 관리에 대한 내용을 적습니다.
 *
 * <p><b>외부 모듈:</b><br>
 * 필요 시 외부 모듈에 대한 내용을 적습니다.
 *
 * @author minhee
 * @since 2026-02-26
 * @see
 */

@Component
class RandomNicknameGenerator {
    private val adjectives = listOf(
        "코딩하는", "행복한", "잠자는", "배고픈", "커피마시는",
        "열일하는", "리팩토링하는", "배포중인", "빌드성공한", "에러없는"
    )

    private val nouns = listOf(
        "개발자", "커피", "버그", "서버", "코드",
        "메모", "플로거", "가비지컬렉터", "널포인터", "데이터베이스"
    )

    private val random = Random()

    /**
     * 무작위 닉네임을 생성합니다.
     * @return 생성된 닉네임 (예: "코딩하는개발자0293")
     */
    fun generate(): String {
        val adjective = adjectives[random.nextInt(adjectives.size)]
        val noun = nouns[random.nextInt(nouns.size)]
        val number = random.nextInt(9000) + 1000

        return "$adjective$noun$number"
    }
}