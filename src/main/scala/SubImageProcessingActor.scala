import java.awt.Color
import java.awt.image.{ColorModel, BufferedImage, Raster}
import java.io.File
import javax.imageio.ImageIO

import akka.actor.{ActorLogging, Actor}
import akka.actor.Actor.Receive
import akka.event.Logging
import ImageDiffActor._

/**
  * Created by vikram on 1/28/16.
  */

object SubImageProcessingActor {
  val white = new Color(255, 255, 255)

  case class Coord(x: Int,
                   y: Int)

  case class SubImageMessage(original: BufferedImage,
                             modified: BufferedImage)
}

class SubImageProcessingActor extends Actor with ActorLogging {
  import SubImageProcessingActor._

  def receive = {
    case SubImageMessage(original, modified) => {
      sender ! new SubImageProcessedMessage(diffImages(original, modified), 1)
    }
    case _ => log.error("Could not understand message")
  }

  def diffImages(b1: BufferedImage, b2: BufferedImage): BufferedImage = {
    //assume that both images are the same size
    val coords = for (x <- 0 until b1.getWidth;
                      y <- 0 until b1.getHeight) yield Coord(x, y)

    val diff = new BufferedImage(b1.getWidth, b1.getHeight, BufferedImage.TYPE_INT_RGB)
    val graphics = diff.createGraphics()
    graphics.setBackground(Color.white)
    graphics.clearRect(0, 0, diff.getWidth, diff.getHeight)
    graphics.dispose()

    (diff /: coords)((accum, coord) => {
      log.info("{},{}", coord.x, coord.y)
      accum.setRGB(coord.x, coord.y, diffPixel(b1.getRGB(coord.x, coord.y), b2.getRGB(coord.x, coord.y)))
      accum
    })
  }

  //if the pixels are different, return p2, otherwise return white
  def diffPixel(p1: Int, p2: Int): Int = if (p1 == p2) white.getRGB else p2

}
