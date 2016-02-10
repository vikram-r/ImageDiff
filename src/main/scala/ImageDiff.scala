import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.io.{IOException, File}
import java.net.URI
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import SubImageProcessingActor._
import ImageDiffActor._

import akka.actor.{Props, ActorSystem}
import akka.pattern.ask
import akka.dispatch.ExecutionContexts._
import akka.util.Timeout

import scala.concurrent.Await
import scala.util.{Failure, Success, Try}

/**
  * Created by vikram on 1/27/16.
  */
object ImageDiff {

  implicit val timeout = Timeout(30, TimeUnit.SECONDS)
  implicit val executionContext = global

  def main(args: Array[String]) {
    if (args.size < 2) exitWithError("2 image file paths required", None)
    println(s"Loading original image: ${args(0)}, and modified image: ${args(1)}")

    val initialImage = loadBufferedImage(args(0)).get
    val modifiedImage = loadBufferedImage(args(1)).get
    val context = ActorSystem("System")

    val startActor = context.actorOf(Props(new ImageDiffActor()))
    val future = startActor ? StartImageDiffMessage(initialImage, modifiedImage)

    val result = Await.result(future, timeout.duration).asInstanceOf[BufferedImage]

    saveBufferedImage(result, "diffed")
    context.terminate()
  }

  def loadBufferedImage(name: String) = Try(ImageIO.read(new File(s"./${name}"))) match {
    case Success(image) => Some(image)
    case Failure(ex) => exitWithError("Could not load image", Some(ex))
                        None
  }

  def saveBufferedImage(image: BufferedImage, name: String) =
    Try(ImageIO.write(image, "jpg", new File(s"./${name}"))) match {
      case Success(image) => Some(image)
      case Failure(ex) => exitWithError("Could not save image", Some(ex))
        None
  }

  def exitWithError(message: String, ex: Option[Throwable]) = {
    println(message)
    ex.foreach(println)
    System.exit(1)
  }
}
