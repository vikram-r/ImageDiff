import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.io.{IOException, File}
import java.net.URI
import javax.imageio.ImageIO

import ImageProcessingActor._

import akka.actor.{Props, ActorSystem}

import scala.util.{Failure, Success, Try}

/**
  * Created by vikram on 1/27/16.
  */
object ImageDiff {

  def main(args: Array[String]) {
    if (args.size < 2) exitWithError("2 image file paths required", None)
    println(s"Loading original image: ${args(0)}, and modified image: ${args(1)}")

    val initialImage = loadBufferedImage(args(0)).get
    val modifiedImage = loadBufferedImage(args(1)).get
    val context = ActorSystem("ImageDiffActors")

    val COMP_SQUARE_SIZE = math.min(math.min(initialImage.getWidth, modifiedImage.getWidth),
                                    math.min(initialImage.getHeight(), modifiedImage.getHeight))
    val SUBIMG_DIM = 50

    //todo this loop is currently dumb, it only iterates to the smallest width or height (instead of both)
    for (i <- 0 until COMP_SQUARE_SIZE by SUBIMG_DIM){
      val size = if (i + (SUBIMG_DIM) > COMP_SQUARE_SIZE) COMP_SQUARE_SIZE - i else SUBIMG_DIM

      context.actorOf(Props(new ImageProcessingActor())) !
        new SubImageMessage(initialImage.getSubimage(i, i, size, size),
                            modifiedImage.getSubimage(i, i, size, size))

    }
  }

  def loadBufferedImage(name: String) = Try(ImageIO.read(new File(s"./${name}"))) match {
    case Success(image) => Some(image)
    case Failure(ex) => exitWithError("Could not load image", Some(ex))
                        None
  }

  def exitWithError(message: String, ex: Option[Throwable]) = {
    println(message)
    ex.foreach(println)
    System.exit(1)
  }
}
