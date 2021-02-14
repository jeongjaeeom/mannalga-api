package com.taskforce.superinvention.app.web.dto.club.album

import com.taskforce.superinvention.app.domain.club.album.ClubAlbum
import com.taskforce.superinvention.app.web.dto.club.ClubWriter
import com.taskforce.superinvention.common.util.aws.s3.S3Path
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

data class ClubAlbumRegisterDto(
        @get:NotBlank(message = "게시판 제목을 입력해주세요")
        val title : String,

        @get:NotNull(message = "이미지가 없습니다.")
        val image : S3Path
)

data class ClubAlbumDto(
        val albumSeq   : Long   = 0,
        val title      : String = "",
        val file_name  : String = "",
        val imgUrl     : String = "",
        val likeCnt    : Long = 0,
        val commentCnt : Long = 0,
        val writer     : ClubWriter,
        val isLiked    : Boolean
) {
        constructor(s3Host: String, clubAlbum: ClubAlbum, isLiked: Boolean): this (
                albumSeq   = clubAlbum.seq!!,
                title      = clubAlbum.title,
                file_name  = clubAlbum.file_name,
                imgUrl     = "${s3Host}/${clubAlbum.img_url}",
                likeCnt    = clubAlbum.albumLikeCnt    ?: 0,
                commentCnt = clubAlbum.albumCommentCnt ?: 0,
                writer     = ClubWriter(clubAlbum.writer),
                isLiked    = isLiked
        )
}

data class ClubAlbumListDto(
        val albumSeq   : Long   = 0,
        val title      : String = "",
        val file_name  : String = "",
        val imgUrl     : String = "",
        val likeCnt    : Long,
        val commentCnt : Long,
        val writerClubUserSeq: Long ?= 0
) {
        constructor(s3Host: String, clubAlbum: ClubAlbum): this (
                albumSeq   = clubAlbum.seq!!,
                title      = clubAlbum.title,
                file_name  = clubAlbum.file_name,
                imgUrl     = "${s3Host}/${clubAlbum.img_url}",
                likeCnt    = clubAlbum.albumLikeCnt ?: 0,
                commentCnt = clubAlbum.albumCommentCnt ?: 0,
                writerClubUserSeq = clubAlbum.writer.seq
        )
}

data class ClubAlbumSearchOption (
        val title: String = ""
)