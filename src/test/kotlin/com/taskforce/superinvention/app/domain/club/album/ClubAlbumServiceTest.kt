package com.taskforce.superinvention.app.domain.club.album

import com.ninjasquad.springmockk.MockkBean
import com.taskforce.superinvention.app.domain.club.Club
import com.taskforce.superinvention.app.domain.club.ClubRepository
import com.taskforce.superinvention.app.domain.club.ClubService
import com.taskforce.superinvention.app.domain.club.album.image.ClubAlbumImageService
import com.taskforce.superinvention.app.domain.club.album.like.ClubAlbumLikeRepository
import com.taskforce.superinvention.app.domain.club.user.ClubUser
import com.taskforce.superinvention.app.domain.club.user.ClubUserRepository
import com.taskforce.superinvention.app.domain.club.user.ClubUserService
import com.taskforce.superinvention.app.domain.role.ClubUserRole
import com.taskforce.superinvention.app.domain.role.Role
import com.taskforce.superinvention.app.domain.role.RoleGroup
import com.taskforce.superinvention.app.domain.role.RoleService
import com.taskforce.superinvention.app.domain.user.User
import com.taskforce.superinvention.app.web.dto.club.album.ClubAlbumEditDto
import com.taskforce.superinvention.app.web.dto.club.album.ClubAlbumRegisterDto
import com.taskforce.superinvention.common.exception.auth.InsufficientAuthException
import com.taskforce.superinvention.common.exception.auth.WithdrawClubUserNotAllowedException
import com.taskforce.superinvention.common.exception.club.ClubNotFoundException
import com.taskforce.superinvention.common.exception.club.UserIsNotClubMemberException
import com.taskforce.superinvention.common.exception.club.album.NoAuthForClubAlbumException
import com.taskforce.superinvention.common.exception.common.IsAlreadyDeletedException
import com.taskforce.superinvention.common.util.aws.s3.S3Path
import com.taskforce.superinvention.config.test.MockkTest
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull

class ClubAlbumServiceTest: MockkTest() {

    lateinit var clubAlbumService: ClubAlbumService

    @MockkBean
    lateinit var roleService: RoleService

    @MockkBean
    lateinit var clubService: ClubService

    @MockkBean
    lateinit var clubUserService: ClubUserService

    @MockkBean
    lateinit var clubAlbumRepository: ClubAlbumRepository

    @MockkBean
    lateinit var clubRepository: ClubRepository

    @MockkBean
    lateinit var clubUserRepository: ClubUserRepository

    @MockkBean
    lateinit var clubAlbumLikeRepository: ClubAlbumLikeRepository

    @MockkBean
    lateinit var clubAlbumImageService: ClubAlbumImageService

    lateinit var nonClubUser   : User
    lateinit var writer        : User
    lateinit var nonWriter     : User
    lateinit var userAsManager : User
    lateinit var userAsMaster  : User

    lateinit var club              : Club
    lateinit var nonWriterClubUser : ClubUser
    lateinit var writerClubUser    : ClubUser
    lateinit var clubManager       : ClubUser
    lateinit var clubMaster        : ClubUser
    lateinit var clubAlbum         : ClubAlbum
    lateinit var deletedClubAlbum  : ClubAlbum
    private  val nonClubSeq = 111L

    private val roleGroup    = RoleGroup("USER", "USER_TYPE")
    private val memberRole   = Role(Role.RoleName.CLUB_MEMBER, roleGroup, 2)
    private val masterRole   = Role(Role.RoleName.MASTER , roleGroup, 4)
    private val managerRole  = Role(Role.RoleName.MANAGER, roleGroup, 3)

    @BeforeEach
    fun setup() {

        clubAlbumService = ClubAlbumService(
            roleService = roleService,
            clubService = clubService,
            clubUserService = clubUserService,
            clubAlbumRepository = clubAlbumRepository,
            clubUserRepository = clubUserRepository,
            clubAlbumLikeRepository = clubAlbumLikeRepository,
            clubAlbumImgService = clubAlbumImageService,
            imgHost = "dummy-aws-s3"
        )


        club = Club(
            name = "테스트 모임",
            description   = "",
            maximumNumber = 10,
            mainImageUrl  = ""
        ).apply { seq = 88 }

        writer        = User ("11111", "모임원").apply { seq = 2 }
        nonWriter     = User ("22222", "작성자가 아닌 모임뭔").apply { seq = 3 }
        userAsManager = User ("33333", "모임 매니저").apply { seq = 4 }
        userAsMaster  = User ("44444", "모임장").apply { seq = 5 }
        nonClubUser   = User ("55555", "모임원이 아닌 유저").apply { seq = 6 }

        writerClubUser    = ClubUser(club, writer   , isLiked = true)
            .apply { seq  = 111 }
            .apply { clubUserRoles = mutableSetOf(ClubUserRole(this, memberRole)) }

        nonWriterClubUser = ClubUser(club, nonWriter, isLiked = true)
            .apply { seq  = 112 }
            .apply { clubUserRoles = mutableSetOf(ClubUserRole(this, memberRole)) }

        clubManager = ClubUser(club, userAsManager, isLiked = true)
            .apply { seq  = 113 }
            .apply { clubUserRoles = mutableSetOf(ClubUserRole(this, managerRole)) }

        clubMaster = ClubUser(club, userAsMaster, isLiked = true)
            .apply { seq  = 114 }
            .apply { clubUserRoles = mutableSetOf(ClubUserRole(this, masterRole)) }

        clubAlbum = ClubAlbum (
            club        = club,
            writer      = writerClubUser,
            title       = "클럽 사진첩 사진 1",
            img_url     = "이미지 URL",
            file_name   = "파일 이름",
            delete_flag = false
        ).apply { seq = 1000 }

        deletedClubAlbum = ClubAlbum (
            club        = club,
            writer      = writerClubUser,
            title       = "삭제된 모임 사진첩 사진",
            img_url     = "이미지 URL",
            file_name   = "파일 이름",
            delete_flag = true
        ).apply { seq = 1001 }
    }

    @Test
    fun `모임원이 아니면 해당 모임 사진첩에 사진을 등록할 수 없음`() {

        // given
        val body = ClubAlbumRegisterDto(
            title = "신규 모임 사진첩 제목",
            image = S3Path(
                absolutePath = "절대경로"
            )
        )

        every { clubService.getValidClubBySeq(club.seq!!)        } returns club

        every { clubUserService.getValidClubUser(club.seq!!, writer)      } returns writerClubUser
        every { clubUserService.getValidClubUser(club.seq!!, nonClubUser) } throws  UserIsNotClubMemberException()

        every { clubAlbumRepository.save( any()) } returns clubAlbum
        every { clubAlbumImageService.registerClubAlbumImage(any(), any()) } returns S3Path()

        assertThrows<UserIsNotClubMemberException> {
            clubAlbumService.registerClubAlbum(nonClubUser, club.seq!!, body)
        }
    }

    @Test
    fun `탈퇴한 상태의 유저도 해당 모임 사진첩에 사진을 등록할 수 없음`() {

        // given
        val body = ClubAlbumRegisterDto(
            title = "신규 모임 사진첩 제목",
            image = S3Path(
                absolutePath = "절대경로"
            )
        )

        every { clubService.getValidClubBySeq(club.seq!!)              } returns club
        every { clubUserService.getValidClubUser(club.seq!!, writer)   } returns writerClubUser
        every { roleService.hasClubMemberAuth(writerClubUser)      } returns false

        assertThrows<WithdrawClubUserNotAllowedException> {
            clubAlbumService.registerClubAlbum(writer, club.seq!!, body)
        }
    }

    @Test
    fun `작성자 이외에는 모임 사진첩을 수정 할 수 없다`() {

        // given
        val body = ClubAlbumEditDto(
            title = "제목",
            image = S3Path(
                absolutePath = "이미지 절대경로",
                filePath     = "이미지 상대경로",
                fileName     = "파일명",
            )
        )

        every { clubService.getValidClubBySeq(club.seq!!) }                returns club
        every { clubUserService.getValidClubUser(club.seq!!, writer) }     returns nonWriterClubUser
        every { clubAlbumRepository.findBySeqWithWriter(clubAlbum.seq!!) } returns clubAlbum

        assertThrows<InsufficientAuthException> {
            clubAlbumService.editClubAlbum(writer, club.seq!!, clubAlbum.seq!!, body)
        }
    }

    @Test
    fun `탈퇴한 모임원은 자신이 작성했던 모임 사진첩을 수정 할 수 없다`() {

        // given
        val body = ClubAlbumEditDto(
            title = "제목",
            image = S3Path(
                absolutePath = "이미지 절대경로",
                filePath     = "이미지 상대경로",
                fileName     = "파일명",
            )
        )

        every { clubService.getValidClubBySeq(club.seq!!) }                returns club
        every { clubUserService.getValidClubUser(club.seq!!, writer) }     returns writerClubUser
        every { clubAlbumRepository.findBySeqWithWriter(clubAlbum.seq!!) } returns clubAlbum
        every { roleService.hasClubMemberAuth(writerClubUser) } returns false

        assertThrows<WithdrawClubUserNotAllowedException> {
            clubAlbumService.editClubAlbum(writer, club.seq!!, clubAlbum.seq!!, body)
        }
    }

    @Test
    fun `모임 사진첩은 사진 게시자와 매니저가 아니면 삭제할 수 없음`() {

        // given
        every { clubService.getValidClubBySeq(club.seq!!)      } returns club
        every { clubService.getValidClubBySeq(neq(club.seq!!)) } throws ClubNotFoundException()

        every { clubUserService.getValidClubUser(club.seq!!, writer)        } returns writerClubUser
        every { clubUserService.getValidClubUser(club.seq!!, nonWriter)     } returns nonWriterClubUser
        every { clubUserService.getValidClubUser(club.seq!!, userAsManager) } returns clubManager
        every { clubUserService.getValidClubUser(club.seq!!, userAsMaster)  } returns clubMaster
        every { clubUserService.getValidClubUser(club.seq!!, nonClubUser)   } throws  UserIsNotClubMemberException()

        // @TODO 해당부분 처음부터 fetch join으로 가져오도록 리팩토링 필요해보임 (매우)
        every { roleService.hasClubManagerAuth(nonWriterClubUser) } returns false
        every { roleService.hasClubMasterAuth(nonWriterClubUser)  } returns false

        every { roleService.hasClubManagerAuth(clubManager) } returns true
        every { roleService.hasClubMasterAuth(clubManager)  } returns false

        every { roleService.hasClubManagerAuth(clubMaster) }  returns true
        every { roleService.hasClubMasterAuth(clubMaster)  }  returns true

        every { clubAlbumRepository.findByIdOrNull(clubAlbum.seq)        } returns clubAlbum
        every { clubAlbumRepository.findByIdOrNull(deletedClubAlbum.seq) } returns deletedClubAlbum

        // when & then

        // 모임이 없을 때
        assertThrows<ClubNotFoundException> {
            clubAlbumService.removeClubAlbum(writer, nonClubSeq, clubAlbum.seq!!)
        }

        // 요청자가 모임원이 아닐 때
        assertThrows<UserIsNotClubMemberException> {
            clubAlbumService.removeClubAlbum(nonClubUser, club.seq!!, clubAlbum.seq!!)
        }

        // 이미 지워진 사진을 삭제할 때
        assertThrows<IsAlreadyDeletedException> {
            clubAlbumService.removeClubAlbum(writer, club.seq!!, deletedClubAlbum.seq!!)
        }

        // 사진 등록자가 삭제할 때
        assertEquals(Unit, clubAlbumService.removeClubAlbum(writer, club.seq!!, clubAlbum.seq!!))
            .run { resetDeleteState(clubAlbum) }

        // 모임장, 매니저가 삭제할 때
        assertEquals(Unit, clubAlbumService.removeClubAlbum(userAsManager, club.seq!!, clubAlbum.seq!!))
            .run { resetDeleteState(clubAlbum) }

        assertEquals(Unit, clubAlbumService.removeClubAlbum(userAsMaster , club.seq!!, clubAlbum.seq!!))
            .run { resetDeleteState(clubAlbum) }

        // 요청자가 모임원이지만 등록자, 모임장, 매니저가 아닐 때
        assertThrows<NoAuthForClubAlbumException> {
            clubAlbumService.removeClubAlbum(nonWriter, club.seq!!, clubAlbum.seq!!)
        }
    }

    private fun resetDeleteState(clubAlbum: ClubAlbum) {
        clubAlbum.delete_flag = false
    }
}
