package me.ramos.idgenerator.application.service

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import me.ramos.idgenerator.adapter.out.persistence.entity.RandomIdGeneratorJpaEntity
import me.ramos.idgenerator.adapter.out.persistence.entity.UsedIdJpaEntity
import me.ramos.idgenerator.application.port.out.CacheIdGeneratorOutPort
import me.ramos.idgenerator.application.port.out.LoadRandomIdOutPort
import me.ramos.idgenerator.application.port.out.LoadUsedIdOutPort
import me.ramos.idgenerator.application.port.out.SaveUsedIdOutPort
import me.ramos.idgenerator.common.component.lock.DistributedLockManager
import me.ramos.idgenerator.common.component.lock.LockCallback
import java.util.concurrent.TimeUnit

class GenerateIdUseCaseTest : BehaviorSpec({

    val distributedLockManager = mockk<DistributedLockManager>()
    val cacheOutPort = mockk<CacheIdGeneratorOutPort>()
    val loadUsedIdOutPort = mockk<LoadUsedIdOutPort>()
    val saveUsedIdOutPort = mockk<SaveUsedIdOutPort>()
    val loadRandomIdOutPort = mockk<LoadRandomIdOutPort>()

    val useCase = GenerateIdUseCase(
        distributedLockManager = distributedLockManager,
        cacheOutPort = cacheOutPort,
        loadUsedIdOutPort = loadUsedIdOutPort,
        saveUsedIdOutPort = saveUsedIdOutPort,
        loadRandomIdOutPort = loadRandomIdOutPort,
    )

    Given("ID 타입이 존재하고 시퀀스가 남아있을 때") {
        val type = "AG"
        val usedIdEntity = UsedIdJpaEntity.create(
            type = type,
            seqIncrement = 7L,
            endSeq = 50_000L,
        )
        val randomIdEntity = mockk<RandomIdGeneratorJpaEntity>()

        every { randomIdEntity.randomNo } returns "A1B2"

        every {
            distributedLockManager.executeWithLock(
                lockName = "ID-GENERATOR:$type",
                waitTime = 5L,
                leaseTime = 3L,
                timeUnit = TimeUnit.SECONDS,
                callback = any<LockCallback<String>>(),
            )
        } answers {
            val callback = arg<LockCallback<String>>(4)
            callback.execute()
        }

        every { cacheOutPort.getOrLoad(type, any()) } returns usedIdEntity
        every { saveUsedIdOutPort.updateSequence(type, any(), any()) } returns 1
        every { cacheOutPort.put(type, usedIdEntity) } returns usedIdEntity
        every { loadRandomIdOutPort.findById(any()) } returns randomIdEntity

        When("ID를 생성하면") {
            val result = useCase.execute(type)

            Then("타입 접두사로 시작하는 ID가 반환된다") {
                result shouldStartWith "$type-"
            }

            Then("랜덤 ID가 포함된다") {
                result shouldBe "$type-A1B2"
            }

            Then("시퀀스가 업데이트된다") {
                verify(exactly = 1) { saveUsedIdOutPort.updateSequence(type, any(), any()) }
            }

            Then("캐시가 업데이트된다") {
                verify(exactly = 1) { cacheOutPort.put(type, usedIdEntity) }
            }
        }
    }
})
