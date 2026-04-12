package me.ramos.idgenerator.application.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.every
import io.mockk.mockk
import me.ramos.idgenerator.adapter.out.persistence.entity.RandomIdGeneratorJpaEntity
import me.ramos.idgenerator.application.exception.IdExhaustedException
import me.ramos.idgenerator.application.port.out.LoadRandomIdOutPort

class GenerateIdUseCaseTest : BehaviorSpec({

    val segmentIdAllocator = mockk<SegmentIdAllocator>()
    val loadRandomIdOutPort = mockk<LoadRandomIdOutPort>()

    val useCase = GenerateIdUseCase(
        segmentIdAllocator = segmentIdAllocator,
        loadRandomIdOutPort = loadRandomIdOutPort,
    )

    Given("ID 타입이 존재하고 시퀀스가 남아있을 때") {
        val type = "AG"
        val randomIdEntity = mockk<RandomIdGeneratorJpaEntity>()

        every { randomIdEntity.randomNo } returns "A1B2"
        every { segmentIdAllocator.nextSequence(type) } returns 7L
        every { loadRandomIdOutPort.findById(7L) } returns randomIdEntity

        When("ID를 생성하면") {
            val result = useCase.execute(type)

            Then("타입 접두사로 시작하는 ID가 반환된다") {
                result shouldStartWith "$type-"
            }

            Then("랜덤 ID가 포함된다") {
                result shouldBe "$type-A1B2"
            }
        }
    }

    Given("시퀀스에 해당하는 랜덤 ID가 없을 때") {
        val type = "AG"

        every { segmentIdAllocator.nextSequence(type) } returns 999999L
        every { loadRandomIdOutPort.findById(999999L) } returns null

        When("ID를 생성하면") {
            Then("IdExhaustedException이 발생한다") {
                shouldThrow<IdExhaustedException> {
                    useCase.execute(type)
                }
            }
        }
    }
})
