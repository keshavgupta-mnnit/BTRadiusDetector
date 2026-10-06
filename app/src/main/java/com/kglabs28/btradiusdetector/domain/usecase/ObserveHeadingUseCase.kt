package com.kglabs28.btradiusdetector.domain.usecase

import com.kglabs28.btradiusdetector.data.CompassRepository
import kotlinx.coroutines.flow.Flow

/** Live compass heading in degrees (0–360). */
class ObserveHeadingUseCase(private val compassRepository: CompassRepository) {

    operator fun invoke(): Flow<Float> = compassRepository.getHeadingFlow()
}
