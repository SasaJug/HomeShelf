package com.jugurdzija.homeshelf.domain.usecases.introseen

import com.jugurdzija.homeshelf.data.onboarding.OnboardingRepository
import javax.inject.Inject

class IntroSeenUseCaseImpl @Inject constructor(
    private val onboardingRepository: OnboardingRepository
) : IntroSeenUseCase {

    override fun hasSeenIntro(): Boolean = onboardingRepository.hasSeenIntro()

    override fun markIntroSeen() = onboardingRepository.markIntroSeen()
}
