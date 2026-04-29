package com.ictglabo.kotomemo.usecase

interface Command<in I, out O> {
    fun execute(input: I): O
}
