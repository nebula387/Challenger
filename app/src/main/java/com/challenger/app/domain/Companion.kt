package com.challenger.app.domain

/**
 * Настроение спутницы на главном экране. Оно и есть вся мотивация:
 * пока дневной план не закрыт — она скучает, по мере выполнения оживает,
 * а когда всё сделано — радуется.
 */
enum class CompanionMood {
    /** Ничего не сделано, а время напоминаний уже прошло. */
    SAD,
    /** День начался, дела ещё впереди. */
    BORED,
    /** Что-то уже сделано. */
    INTERESTED,
    /** Всё обязательное закрыто. */
    HAPPY,
    /** Закрыт весь день целиком. */
    CELEBRATING;

    val isPositive: Boolean get() = this >= INTERESTED
}

object Companion {

    /**
     * Настроение растёт постепенно, как и просили: чем больше челленджей закрыто,
     * тем она довольнее. Обязательные весят больше остальных.
     */
    fun moodFor(
        doneCount: Int,
        total: Int,
        mustLeft: Int,
        anyOverdue: Boolean
    ): CompanionMood {
        if (total == 0) return CompanionMood.INTERESTED
        if (doneCount == total) return CompanionMood.CELEBRATING
        if (mustLeft == 0) return CompanionMood.HAPPY

        return when {
            doneCount > 0 -> CompanionMood.INTERESTED
            anyOverdue -> CompanionMood.SAD
            else -> CompanionMood.BORED
        }
    }
}
