package me.ramos.idgenerator.domain.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveLength

class IdGenerationUtilTest : BehaviorSpec({

    Given("Base33 인코딩/디코딩") {

        When("0을 Base33으로 변환하면") {
            val result = IdGenerationUtil.toBase33(0)

            Then("'0'을 반환한다") {
                result shouldBe "0"
            }
        }

        When("33을 Base33으로 변환하면") {
            val result = IdGenerationUtil.toBase33(33)

            Then("'10'을 반환한다") {
                result shouldBe "10"
            }
        }

        When("1을 패딩된 Base33으로 변환하면") {
            val result = IdGenerationUtil.toBase33Padded(1)

            Then("4자리로 패딩된다") {
                result shouldHaveLength 4
                result shouldBe "0001"
            }
        }

        When("100000을 Base33으로 변환하면") {
            val result = IdGenerationUtil.toBase33(100_000)

            Then("디코딩하면 원래 값으로 복원된다") {
                IdGenerationUtil.toDecimal(result) shouldBe 100_000
            }
        }

        When("모든 유효 문자를 디코딩하면") {
            val validChars = "0123456789ABCDFGHJKLMNPQRSTUVWXYZ"

            Then("각 문자가 0~32 사이 값에 매핑된다") {
                validChars.forEachIndexed { index, char ->
                    IdGenerationUtil.toDecimal(char.toString()) shouldBe index.toLong()
                }
            }
        }
    }

    Given("Base33 라운드트립 검증") {

        When("1부터 1000까지 변환/역변환하면") {

            Then("모든 값이 원래 값으로 복원된다") {
                (1L..1000L).forEach { original ->
                    val encoded = IdGenerationUtil.toBase33(original)
                    val decoded = IdGenerationUtil.toDecimal(encoded)
                    decoded shouldBe original
                }
            }
        }
    }

    Given("경계값 검증") {

        When("Base33 최대 4자리 값(33^4 - 1 = 1185920)을 변환하면") {
            val maxFourDigit = 33L * 33 * 33 * 33 - 1
            val result = IdGenerationUtil.toBase33Padded(maxFourDigit)

            Then("4자리로 표현된다") {
                result shouldHaveLength 4
                result shouldBe "ZZZZ"
            }
        }
    }
})
