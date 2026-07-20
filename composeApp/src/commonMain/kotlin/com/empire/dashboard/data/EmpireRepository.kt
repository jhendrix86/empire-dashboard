package com.empire.dashboard.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class EmpireRepository(private val api: EmpireApi = EmpireApi()) {

    fun statusStream(intervalMs: Long = 5000L): Flow<EmpireState> = flow {
        while (true) {
            emit(EmpireState.Loading)
            val result = api.fetchStatus()
            result.fold(
                onSuccess = { emit(EmpireState.Success(it)) },
                onFailure = { emit(EmpireState.Error(it.message ?: "Connection failed")) }
            )
            delay(intervalMs)
        }
    }
}

sealed class EmpireState {
    data object Loading : EmpireState()
    data class Success(val status: EmpireStatus) : EmpireState()
    data class Error(val message: String) : EmpireState()
}
