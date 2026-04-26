package com.masolodilov.jericho.model

sealed class CureResult {
    data class Success(
        val statusTitle: String,
        val itemTitle: String,
    ) : CureResult()

    data class Unavailable(
        val reason: String,
    ) : CureResult()
}
