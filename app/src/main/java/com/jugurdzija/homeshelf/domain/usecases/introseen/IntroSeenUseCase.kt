package com.jugurdzija.homeshelf.domain.usecases.introseen

interface IntroSeenUseCase {
    fun hasSeenIntro(): Boolean
    fun markIntroSeen()
}
