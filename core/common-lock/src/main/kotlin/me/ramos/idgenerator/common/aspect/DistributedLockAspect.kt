package me.ramos.idgenerator.common.aspect

import io.github.oshai.kotlinlogging.KotlinLogging
import me.ramos.idgenerator.common.annotation.DistributedLock
import me.ramos.idgenerator.common.component.lock.DistributedLockManager
import me.ramos.idgenerator.common.component.lock.LockCallback
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}
private const val LOCK_PREFIX = "LOCK:"

@Aspect
@Component
class DistributedLockAspect(
    private val distributedLockManager: DistributedLockManager,
) {

    private val parser = SpelExpressionParser()

    @Around("@annotation(distributedLock)")
    fun around(joinPoint: ProceedingJoinPoint, distributedLock: DistributedLock): Any? {
        val lockKey = LOCK_PREFIX + resolveKey(joinPoint, distributedLock.key)
        log.debug { "분산락 요청: key=$lockKey" }

        return distributedLockManager.executeWithLock(
            lockName = lockKey,
            waitTime = distributedLock.waitTime,
            leaseTime = distributedLock.leaseTime,
            timeUnit = distributedLock.timeUnit,
            callback = LockCallback { joinPoint.proceed() },
        )
    }

    private fun resolveKey(joinPoint: ProceedingJoinPoint, expression: String): String {
        val signature = joinPoint.signature as MethodSignature
        val parameterNames = signature.parameterNames
        val args = joinPoint.args

        val context = StandardEvaluationContext().apply {
            parameterNames.forEachIndexed { index, name ->
                setVariable(name, args[index])
            }
        }

        return parser.parseExpression(expression).getValue(context, String::class.java)
            ?: throw IllegalArgumentException("SpEL 표현식 결과가 null: expression=$expression")
    }
}
