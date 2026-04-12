package me.ramos.idgenerator.adapter.out.persistence.repository

/**
 * UsedId QueryDSL 커스텀 리포지토리 인터페이스.
 *
 * @author HakHyeon Song
 */
interface UsedIdRepositoryCustom {
    fun updateSequence(type: String, currentSeq: Long, count: Long): Long
    fun advanceToNextRange(type: String, seqIncrement: Long, endSeq: Long): Long
    fun isIdGenerationRequired(totalRandomIdCount: Long?): Boolean
}
