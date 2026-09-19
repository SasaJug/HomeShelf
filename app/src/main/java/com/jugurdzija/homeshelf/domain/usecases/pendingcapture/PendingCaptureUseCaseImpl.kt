package com.jugurdzija.homeshelf.domain.usecases.pendingcapture

import android.graphics.Bitmap
import com.jugurdzija.homeshelf.data.pendingcapture.PendingCaptureRepository
import javax.inject.Inject

class PendingCaptureUseCaseImpl @Inject constructor(
    private val pendingCaptureRepository: PendingCaptureRepository
) : PendingCaptureUseCase {

    override suspend fun save(bitmap: Bitmap) = pendingCaptureRepository.save(bitmap)

    override suspend fun load(): Bitmap? = pendingCaptureRepository.load()

    override suspend fun clear() = pendingCaptureRepository.clear()
}
