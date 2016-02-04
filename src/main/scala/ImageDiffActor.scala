import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

import akka.actor.{Props, ActorLogging, Actor}
import SubImageProcessingActor._


/**
  * Created by vikram on 1/31/16.
  */

object ImageDiffActor {

  val MAX_SUBIMAGE_DIM = 50 //the largest possible subimage dimension (MAX_SUBIMAGE_DIM x MAX_SUBIMAGE_DIM)

  case class StartImageDiffMessage(original: BufferedImage,
                             modified: BufferedImage)

  case class SubImageProcessedMessage(diffed: BufferedImage,
                                      location: Int) //todo figure out what this is, maybe coords
}

class ImageDiffActor extends Actor with ActorLogging{
  import ImageDiffActor._

  var diffImageDim: Int = _ //the dimension of the square box that will be diffed
  var subImagesSent:Int = _
  val subImagesReceived: Map[Int, BufferedImage] = Map.empty //a map of subImage locations to the subImage

  def receive = {
    case StartImageDiffMessage(original, modified) => {
      //recreate original with the same size as modified. Crop or add white background if necessary
      val resizedOriginal = new BufferedImage(modified.getWidth, modified.getHeight, BufferedImage.TYPE_INT_RGB)
      val graphics = resizedOriginal.createGraphics()
      graphics.setBackground(Color.white)
      graphics.clearRect(0, 0, resizedOriginal.getWidth, resizedOriginal.getHeight)
      graphics.drawImage(original, 0, 0, null)
      graphics.dispose()

      val diffImageWidth = resizedOriginal.getWidth
      val diffImageHeight = resizedOriginal.getHeight

      for (x <- 0 until diffImageWidth by MAX_SUBIMAGE_DIM;
           y <- 0 until diffImageHeight by MAX_SUBIMAGE_DIM
          ) {
        val width = if (x + MAX_SUBIMAGE_DIM > diffImageWidth) diffImageWidth - x else MAX_SUBIMAGE_DIM
        val height = if (y + MAX_SUBIMAGE_DIM > diffImageHeight) diffImageHeight - y else MAX_SUBIMAGE_DIM

        subImagesSent += 1
        context.actorOf(Props(new SubImageProcessingActor())) !
          new SubImageMessage(resizedOriginal.getSubimage(x, y, width, height),
            modified.getSubimage(x, y, width, height))
      }

      log.info(s"${subImagesSent} partitions sent")
    }

    case SubImageProcessedMessage(diffed, location) => {


    }
    case _ => log.error("Unknown message, could not start")
  }
}
