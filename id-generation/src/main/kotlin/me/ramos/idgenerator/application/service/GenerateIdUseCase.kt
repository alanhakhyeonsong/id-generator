package me.ramos.idgenerator.application.service

import me.ramos.idgenerator.application.exception.IdExhaustedException
import me.ramos.idgenerator.application.port.`in`.GenerateIdInPort
import me.ramos.idgenerator.application.port.out.LoadRandomIdOutPort
import org.springframework.stereotype.Service

/**
 * ID 생성 유스케이스.
 *
 * [SegmentIdAllocator]를 통해 사전 할당된 블록에서 시퀀스를 채번하고,
 * 해당 시퀀스에 매핑된 랜덤 ID를 조회하여 반환한다.
 *
 * @author HakHyeon Song
 */
@Service
class GenerateIdUseCase(
    private val segmentIdAllocator: SegmentIdAllocator,
    private val loadRandomIdOutPort: LoadRandomIdOutPort,
) : GenerateIdInPort {

    override fun execute(type: String): String {
        val seq = segmentIdAllocator.nextSequence(type)

        val randomId = loadRandomIdOutPort.findById(seq)
            ?: throw IdExhaustedException(type)

        return "$type-${randomId.randomNo}"
    }
}
