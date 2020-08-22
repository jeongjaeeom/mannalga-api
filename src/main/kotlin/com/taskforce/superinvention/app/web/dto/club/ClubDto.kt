package com.taskforce.superinvention.app.web.dto.club

import com.taskforce.superinvention.app.domain.club.Club
import com.taskforce.superinvention.app.domain.user.User
import java.time.LocalDateTime
import javax.swing.plaf.nimbus.State

class ClubDto (
        var seq: Long?,
        var name: String,
        var description: String,
        var maximumNumber: Long,
        var userCount: Long
){
    constructor(club : Club, userCount: Long){
        this.seq = club.seq
        this.name = club.name
        this.description = club.description
        this.maximumNumber = club.maximumNumber
        this.userCount = userCount
    }
}