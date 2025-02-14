package com.taskforce.superinvention.app.domain.user.userRegion

import com.taskforce.superinvention.app.domain.region.RegionRepository
import com.taskforce.superinvention.app.domain.user.User
import com.taskforce.superinvention.app.web.dto.region.SimpleRegionDto
import com.taskforce.superinvention.app.web.dto.region.RegionRequestDto
import com.taskforce.superinvention.app.web.dto.region.RegionWithPriorityDto
import com.taskforce.superinvention.app.web.dto.user.UserRegionDto
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserRegionService(
        private val userRegionRepository: UserRegionRepository,
        private val regionRepository: RegionRepository
) {

    @Transactional
    fun findUserRegionList(user: User): UserRegionDto {
        val userRegions: List<UserRegion> = userRegionRepository.findByUserWithRegion(user.seq!!)

        return when(userRegions.isEmpty()) {
            true -> UserRegionDto(user, emptyList())
            else -> UserRegionDto(user, userRegions.map { e -> RegionWithPriorityDto(SimpleRegionDto(e.region), e.priority) }.toList())
        }
    }

    /**
     * 유저 지역 정보 저장
     */
    @Transactional
    fun changeUserRegion(user: User, regions: List<RegionRequestDto>): UserRegionDto {
        if (user.seq == null) throw NullPointerException()

        val userRegionList: List<UserRegion> = userRegionRepository.findByUserSeq(user.seq!!)
        userRegionRepository.deleteAll(userRegionList)

        val toAdd = regions.map { e -> UserRegion(user, regionRepository.findByIdOrNull(e.seq) ?: throw NullPointerException(), e.priority) }.toMutableList()
        userRegionRepository.saveAll(toAdd)

        return findUserRegionList(user)
    }
}