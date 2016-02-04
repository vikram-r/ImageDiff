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

      diffImageDim = math.min(math.min(original.getWidth, modified.getWidth),
        math.min(original.getHeight(), modified.getHeight))

      //todo this loop is currently dumb, it only iterates to the smallest width or height (instead of both)
      for (i <- 0 until diffImageDim by MAX_SUBIMAGE_DIM){
        val size = if (i + (MAX_SUBIMAGE_DIM) > diffImageDim) diffImageDim - i else MAX_SUBIMAGE_DIM
        subImagesSent += 1
        context.actorOf(Props(new SubImageProcessingActor())) !
          new SubImageMessage(original.getSubimage(i, i, size, size),
            modified.getSubimage(i, i, size, size))
      }
    }

    case SubImageProcessedMessage(diffed, location) => {


    }
    case _ => log.error("Unknown message, could not start")
  }
}
