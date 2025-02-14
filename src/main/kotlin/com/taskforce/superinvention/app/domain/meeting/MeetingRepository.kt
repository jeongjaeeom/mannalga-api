package com.taskforce.superinvention.app.domain.meeting

import com.taskforce.superinvention.app.domain.club.QClub
import com.taskforce.superinvention.app.domain.club.user.ClubUser
import com.taskforce.superinvention.app.domain.club.user.QClubUser
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface MeetingRepository : JpaRepository<Meeting, Long>, MeetingRepositoryCustom {
}


interface MeetingRepositoryCustom {
    fun findMeetingApplicationByUserAndMeetingSeq(clubUser: ClubUser, meetingSeq: Long): MeetingApplication
    fun getPagedMeetings(clubSeq: Long, pageable: Pageable): Page<Meeting>
}

@Repository
class MeetingRepositoryImpl : QuerydslRepositorySupport(Meeting::class.java), MeetingRepositoryCustom{

    @Transactional(readOnly = true)
    override fun getPagedMeetings(clubSeq: Long, pageable: Pageable): Page<Meeting> {
        val query = from(QMeeting.meeting)
                .join(QMeeting.meeting.club, QClub.club)
                .fetchJoin()
                .where(QClub.club.seq.eq(clubSeq)
                        , QMeeting.meeting.deleteFlag.isFalse
                )

        if (pageable != Pageable.unpaged()) {
            query.offset(pageable.offset)
                .limit(pageable.pageSize.toLong())
        }

        val fetchResult = query
                .orderBy(QMeeting.meeting.startTimestamp.desc())
                .fetchResults()

        return PageImpl(fetchResult.results, pageable, fetchResult.total)
    }

    @Transactional
    override fun findMeetingApplicationByUserAndMeetingSeq(clubUserParam: ClubUser, meetingSeq: Long): MeetingApplication {
        val meetingApplication = QMeetingApplication.meetingApplication
        val meeting = QMeeting.meeting
        val clubUser = QClubUser.clubUser
        val club = QClub.club

        return from(meetingApplication)
            .join(meetingApplication.meeting, meeting)
            .join(meetingApplication.clubUser, clubUser)
            .join(meeting.club, club)
            .where(
                clubUser.seq.eq(clubUserParam.seq), meeting.seq.eq(meetingSeq)
            ).fetchOne()
    }
}