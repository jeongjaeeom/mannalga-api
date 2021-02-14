package com.taskforce.superinvention.app.domain.club.board.img

import com.taskforce.superinvention.app.domain.club.board.ClubBoard
import com.taskforce.superinvention.app.domain.common.image.webp.convert.WebpConvertService
import com.taskforce.superinvention.common.util.aws.s3.AwsS3Mo
import com.taskforce.superinvention.common.util.aws.s3.S3Path
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ClubBoardImgService(
        private val awsS3Mo: AwsS3Mo,
        private val clubBoardImgRepository: ClubBoardImgRepository,
        private val webpConvertService: WebpConvertService
) {

    /**
     * 글 등록시, 이미지 같이 등록
     */
    @Transactional
    fun registerImg(clubBoard: ClubBoard, imgList: List<S3Path>): List<ClubBoardImg> {
        val imgFolder = "clubBoardImg/${clubBoard.seq}"

        // [1] 기존 임시 저장된 파일들을 해당 폴더로 이동
        val clubBoardImgList = imgList.map{ s3Path ->
            val movedFile: S3Path = awsS3Mo.moveFile(s3Path, "$imgFolder/${s3Path.fileName}")
            val webpFile : S3Path = webpConvertService.convertToWebP(movedFile)

            ClubBoardImg(
                imgName   = movedFile.fileName,
                imgUrl    = movedFile.filePath,
                clubBoard  = clubBoard,
                deleteFlag = false
            )
        }

        clubBoardImgRepository.saveAll(clubBoardImgList)
        return clubBoardImgList
    }

    @Transactional
    fun deleteImages(clubBoard: ClubBoard) {
        val imgList =  clubBoardImgRepository.findByClubBoard(clubBoard)

        imgList.forEach{ clubBoardImg -> clubBoardImg.deleteFlag = true}

        // [1] 해당 이미지 DB에서 제거 / flag 처리
        clubBoardImgRepository.saveAll(imgList)
    }
}