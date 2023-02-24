package com.c5000.mastery.database

import _root_.java.awt.Image
import _root_.java.awt.image.BufferedImage
import _root_.java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import _root_.javax.imageio.stream.MemoryCacheImageOutputStream
import _root_.javax.imageio.{ImageWriteParam, IIOImage, ImageWriter, ImageIO}
import com.c5000.mastery.shared.{FileParts, Config}
import java.util.UUID

object FileActions {

    private val imageContentTypes = List[String](
        "image/png",
        "image/jpg",
        "image/jpeg",
        "image/gif",
        "image/bmp"
    )

    def isImage(contentType: String): Boolean = {
        return imageContentTypes.contains(contentType)
    }

    def saveImage(id: UUID, stream: InputStream) {
        val image = ImageIO.read(stream)
        val hiresImage = resize(image, Config.MAX_UPLOADED_IMAGE_WIDTH_HIRES, Config.MAX_UPLOADED_IMAGE_HEIGHT_HIRES, true)
        val largeImage = resize(hiresImage, Config.MAX_UPLOADED_IMAGE_WIDTH_LARGE, Config.MAX_UPLOADED_IMAGE_HEIGHT_LARGE, false)
        val mediumImage = resize(largeImage, Config.MAX_UPLOADED_IMAGE_WIDTH_MEDIUM, Config.MAX_UPLOADED_IMAGE_HEIGHT_MEDIUM, false)
        val smallImage = resize(mediumImage, Config.MAX_UPLOADED_IMAGE_WIDTH_SMALL, Config.MAX_UPLOADED_IMAGE_HEIGHT_SMALL, false)
        Database.saveFile(id, FileParts.HIRES, convertToJpeg(hiresImage), "image/jpeg")
        Database.saveFile(id, FileParts.LARGE, convertToJpeg(largeImage), "image/jpeg")
        Database.saveFile(id, FileParts.MEDIUM, convertToJpeg(mediumImage), "image/jpeg")
        Database.saveFile(id, FileParts.SMALL, convertToJpeg(smallImage), "image/jpeg")
    }

    def deleteImage(id: UUID) {
        Database.deleteFile(id, FileParts.HIRES)
        Database.deleteFile(id, FileParts.LARGE)
        Database.deleteFile(id, FileParts.MEDIUM)
        Database.deleteFile(id, FileParts.SMALL)
    }

    private def resize(sourceImage: BufferedImage, maxWidth: Int, maxHeight: Int, scaleLargestAxis: Boolean): BufferedImage = {
        var resizedImage: Image = sourceImage

        val exceedsOneAxis = sourceImage.getWidth > maxWidth || sourceImage.getHeight > maxHeight
        val exceedsBothAxis = sourceImage.getWidth > maxWidth && sourceImage.getHeight > maxHeight
        if ((scaleLargestAxis && exceedsOneAxis) || (!scaleLargestAxis && exceedsBothAxis)) {
            if (sourceImage.getWidth >= sourceImage.getHeight) {
                if (scaleLargestAxis) {
                    resizedImage = sourceImage.getScaledInstance(maxWidth, -1, Image.SCALE_SMOOTH)
                }
                else {
                    resizedImage = sourceImage.getScaledInstance(-1, maxHeight, Image.SCALE_SMOOTH)
                }
            }
            else {
                if (scaleLargestAxis) {
                    resizedImage = sourceImage.getScaledInstance(-1, maxHeight, Image.SCALE_SMOOTH)
                }
                else {
                    resizedImage = sourceImage.getScaledInstance(maxWidth, -1, Image.SCALE_SMOOTH)
                }
            }
        }

        val bufferedImage = new BufferedImage(
            resizedImage.getWidth(null),
            resizedImage.getHeight(null),
            BufferedImage.TYPE_INT_RGB)
        bufferedImage.getGraphics.drawImage(resizedImage, 0, 0, null)
        return bufferedImage
    }

    private def convertToJpeg(bufferedImage: BufferedImage): ByteArrayInputStream = {
        val iter = ImageIO.getImageWritersByFormatName("jpeg")
        val writer = iter.next().asInstanceOf[ImageWriter]

        val iwp = writer.getDefaultWriteParam
        iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT)
        iwp.setCompressionQuality(0.9f)

        val os = new ByteArrayOutputStream
        val ios = new MemoryCacheImageOutputStream(os)
        writer.setOutput(ios)
        val image = new IIOImage(bufferedImage, null, null)
        writer.write(null, image, iwp)
        writer.dispose()

        return new ByteArrayInputStream(os.toByteArray)
    }

    def recreateSmallImages(file: DatabaseFile, stream: InputStream) {
        try {
            val hiresImage = ImageIO.read(stream)

            val largeImage = resize(hiresImage, Config.MAX_UPLOADED_IMAGE_WIDTH_LARGE, Config.MAX_UPLOADED_IMAGE_HEIGHT_LARGE, false)
            val mediumImage = resize(largeImage, Config.MAX_UPLOADED_IMAGE_WIDTH_MEDIUM, Config.MAX_UPLOADED_IMAGE_HEIGHT_MEDIUM, false)
            val smallImage = resize(mediumImage, Config.MAX_UPLOADED_IMAGE_WIDTH_SMALL, Config.MAX_UPLOADED_IMAGE_HEIGHT_SMALL, false)

            Database.deleteFile(file.id, FileParts.LARGE)
            Database.saveFile(file.id, FileParts.LARGE, convertToJpeg(largeImage), "image/jpeg")
            Database.deleteFile(file.id, FileParts.MEDIUM)
            Database.saveFile(file.id, FileParts.MEDIUM, convertToJpeg(mediumImage), "image/jpeg")
            Database.deleteFile(file.id, FileParts.SMALL)
            Database.saveFile(file.id, FileParts.SMALL, convertToJpeg(smallImage), "image/jpeg")
        }
        catch {
            case _ => {}
        }
        finally {
            stream.close()
        }
    }
}