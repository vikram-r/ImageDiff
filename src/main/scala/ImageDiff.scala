import java.awt.image.BufferedImage
import java.io.{IOException, File}
import java.net.URI
import javax.imageio.ImageIO

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

    println(s"${initialImage.getWidth}")

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
