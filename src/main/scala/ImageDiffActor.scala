import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import SubImageProcessingActor._


/**
  * Created by vikram on 1/31/16.
  */

object ImageDiffActor {

  val MAX_SUBIMAGE_DIM = 50 //the largest possible subimage dimension for width and height

  case class StartImageDiffMessage(original: BufferedImage,
                             modified: BufferedImage)

  case class SubImageProcessedMessage(diffed: BufferedImage,
                                      location: Coord)

  case class Coord(x: Int,
                   y: Int)
}

class ImageDiffActor extends Actor with ActorLogging{
  import ImageDiffActor._

  private var startActor: Option[ActorRef] = None

  private var diffImageWidth: Int = _
  private var diffImageHeight: Int = _

  private var subImagesSent: Int = _
  private var subImagesReceived: Map[Coord, BufferedImage] = Map.empty //a map of subImage locations to the subImage

  def receive = {
    case StartImageDiffMessage(original, modified) => {
      startActor = Some(sender)

      //recreate original with the same size as modified. Crop or add white background if necessary
      val resizedOriginal = new BufferedImage(modified.getWidth, modified.getHeight, BufferedImage.TYPE_INT_RGB)
      diffImageWidth = resizedOriginal.getWidth
      diffImageHeight = resizedOriginal.getHeight

      val graphics = resizedOriginal.createGraphics()
      graphics.setBackground(Color.white)
      graphics.clearRect(0, 0, diffImageWidth, diffImageHeight)
      graphics.drawImage(original, 0, 0, null)
      graphics.dispose()

      /*
       * don't send the messages in the loop, because it could lead to a
       * race condition if earlier SubImageProcessingActors finish before this loop
       * terminates. (subImagesReceived could be equal to subImagesSent before all
       * messages are sent)
       */
      {
        for (x <- 0 until diffImageWidth by MAX_SUBIMAGE_DIM;
             y <- 0 until diffImageHeight by MAX_SUBIMAGE_DIM
        ) yield {
          val width = if (x + MAX_SUBIMAGE_DIM > diffImageWidth) diffImageWidth - x else MAX_SUBIMAGE_DIM
          val height = if (y + MAX_SUBIMAGE_DIM > diffImageHeight) diffImageHeight - y else MAX_SUBIMAGE_DIM

          subImagesSent += 1
          new SubImageMessage(resizedOriginal.getSubimage(x, y, width, height),
            modified.getSubimage(x, y, width, height), Coord(x, y))
        }
      }.foreach(message => context.actorOf(Props(new SubImageProcessingActor())) ! message)

      log.info(s"${subImagesSent} partitions sent")
    }

    case SubImageProcessedMessage(diffed, location) => {
      log.info(s"Received diffed partition for ${location}")
      subImagesReceived += (location -> diffed)

      if (subImagesReceived.size == subImagesSent) {
        log.info(s"${subImagesReceived.size} partitions received")
        startActor.foreach(_ ! combine)
      }
    }

    case _ => log.error("Unknown message, could not start")
  }

  def combine(): BufferedImage = {
    val diffImage = new BufferedImage(diffImageWidth, diffImageHeight, BufferedImage.TYPE_INT_RGB)
    val graphics = diffImage.createGraphics()
    graphics.setBackground(Color.white)
    graphics.clearRect(0, 0, diffImageWidth, diffImageHeight)

    subImagesReceived.foreach(entry => graphics.drawImage(entry._2, entry._1.x, entry._1.y, null))
    graphics.dispose()
    diffImage
  }
}
