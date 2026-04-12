package me.ramos.idgenerator.domain.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import me.ramos.idgenerator.adapter.out.persistence.entity.UsedIdJpaEntity

class UsedIdTypeInfoTest : BehaviorSpec({

    Given("UsedIdTypeInfo가 초기 상태일 때") {
        val entity = UsedIdJpaEntity.create(
            type = "TS",
            seqIncrement = 7L,
            endSeq = 50_000L,
        )
        val typeInfo = UsedIdTypeInfo(entity)

        When("nextSequence()를 호출하면") {
            val seq = typeInfo.nextSequence()

            Then("0이 아닌 시퀀스가 생성된다") {
                seq shouldBeGreaterThan 0
            }

            Then("count가 1 증가한다") {
                entity.count shouldBe 1
            }
        }
    }

    Given("시퀀스가 소진되지 않았을 때") {
        val entity = UsedIdJpaEntity.create(
            type = "TS",
            seqIncrement = 7L,
            endSeq = 50_000L,
        )
        val typeInfo = UsedIdTypeInfo(entity)

        When("isExhausted()를 확인하면") {

            Then("false를 반환한다") {
                typeInfo.isExhausted() shouldBe false
            }
        }
    }

    Given("연속으로 시퀀스를 생성할 때") {
        val entity = UsedIdJpaEntity.create(
            type = "TS",
            seqIncrement = 7L,
            endSeq = 50_000L,
        )
        val typeInfo = UsedIdTypeInfo(entity)

        When("10개의 시퀀스를 생성하면") {
            val sequences = (1..10).map { typeInfo.nextSequence() }

            Then("모든 시퀀스가 고유하다") {
                sequences.distinct().size shouldBe 10
            }

            Then("count가 10이다") {
                entity.count shouldBe 10
            }
        }
    }
})
