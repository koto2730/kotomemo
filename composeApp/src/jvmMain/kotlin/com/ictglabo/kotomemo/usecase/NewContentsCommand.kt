package com.ictglabo.kotomemo.usecase

import com.ictglabo.kotomemo.entity.Contents

class NewContentsCommand : Command<Unit, Contents> {
    override fun execute(input: Unit): Contents = Contents.empty()
}
