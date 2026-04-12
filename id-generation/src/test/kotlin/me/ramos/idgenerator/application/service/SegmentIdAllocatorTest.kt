package me.ramos.idgenerator.application.service

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import me.ramos.idgenerator.adapter.out.persistence.entity.UsedIdJpaEntity
import me.ramos.idgenerator.application.port.out.CacheIdGeneratorOutPort
import me.ramos.idgenerator.application.port.out.LoadUsedIdOutPort
import me.ramos.idgenerator.application.port.out.SaveUsedIdOutPort
import me.ramos.idgenerator.common.component.lock.DistributedLockManager
import me.ramos.idgenerator.common.component.lock.LockCallback
import java.util.concurrent.TimeUnit

class SegmentIdAllocatorTest : BehaviorSpec({

    val distributedLockManager = mockk<DistributedLockManager>()
    val cacheOutPort = mockk<CacheIdGeneratorOutPort>()
    val loadUsedIdOutPort = mockk<LoadUsedIdOutPort>()
    val saveUsedIdOutPort = mockk<SaveUsedIdOutPort>()

    val allocator = SegmentIdAllocator(
        distributedLockManager = distributedLockManager,
        cacheOutPort = cacheOutPort,
        loadUsedIdOutPort = loadUsedIdOutPort,
        saveUsedIdOutPort = saveUsedIdOutPort,
    )

    beforeSpec {
        every {
            distributedLockManager.executeWithLock(
                lockName = any(),
                waitTime = any(),
                leaseTime = any(),
                timeUnit = any(),
                callback = any<LockCallback<IdSegment>>(),
            )
        } answers {
            val callback = arg<LockCallback<IdSegment>>(4)
            callback.execute()
        }
    }

    Given("ID 타입이 존재하고 블록 할당이 가능할 때") {
        val type = "TS"
        val entity = UsedIdJpaEntity.create(
            type = type,
            seqIncrement = 7L,
            endSeq = 50_000L,
        )

        every { cacheOutPort.getOrLoad(type, any()) } returns entity
        every { saveUsedIdOutPort.updateSequence(type, any(), any()) } returns 1
        every { cacheOutPort.put(type, entity) } returns entity

        When("시퀀스를 요청하면") {
            val seq = allocator.nextSequence(type)

            Then("유효한 시퀀스가 반환된다") {
                seq shouldBeGreaterThan 0
            }

            Then("DB에 블록 단위로 시퀀스가 업데이트된다") {
                verify(exactly = 1) {
                    saveUsedIdOutPort.updateSequence(type, any(), eq(SegmentIdAllocator.BLOCK_SIZE))
                }
            }
        }

        When("동일 타입으로 연속 요청하면") {
            val sequences = (1..10).map { allocator.nextSequence(type) }

            Then("모든 시퀀스가 고유하다") {
                sequences.distinct() shouldHaveSize 10
            }

            Then("분산 락은 블록 할당 시 1회만 호출된다") {
                verify(atMost = 2) {
                    distributedLockManager.executeWithLock(
                        lockName = "ID-SEGMENT:$type",
                        waitTime = 10L,
                        leaseTime = 5L,
                        timeUnit = TimeUnit.SECONDS,
                        callback = any<LockCallback<IdSegment>>(),
                    )
                }
            }
        }
    }

    Given("IdSegment 단위 테스트") {
        When("시퀀스 블록을 생성하면") {
            val sequences = longArrayOf(100L, 200L, 300L)
            val segment = IdSegment(sequences)

            Then("순서대로 시퀀스가 반환된다") {
                segment.next() shouldBe 100L
                segment.next() shouldBe 200L
                segment.next() shouldBe 300L
            }

            Then("소진 후 null이 반환된다") {
                segment.next() shouldBe null
            }
        }

        When("remaining을 확인하면") {
            val segment = IdSegment(longArrayOf(1L, 2L, 3L))
            segment.next()

            Then("잔여 수가 정확하다") {
                segment.remaining() shouldBe 2
            }
        }
    }
})
