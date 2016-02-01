import java.awt.image.BufferedImage

import akka.actor.{Props, ActorLogging, Actor}
import SubImageProcessingActor._


/**
  * Created by vikram on 1/31/16.
  */

object ImageDiffActor {
  case class StartImageDiffMessage(original: BufferedImage,
                             modified: BufferedImage)
}

class ImageDiffActor() extends Actor with ActorLogging{
  import ImageDiffActor._

  def receive = {
    case StartImageDiffMessage(original, modified) => {
      val COMP_SQUARE_SIZE = math.min(math.min(original.getWidth, modified.getWidth),
        math.min(original.getHeight(), modified.getHeight))
      val SUBIMG_DIM = 50

      //todo this loop is currently dumb, it only iterates to the smallest width or height (instead of both)
      for (i <- 0 until COMP_SQUARE_SIZE by SUBIMG_DIM){
        val size = if (i + (SUBIMG_DIM) > COMP_SQUARE_SIZE) COMP_SQUARE_SIZE - i else SUBIMG_DIM

        context.actorOf(Props(new SubImageProcessingActor())) !
          new SubImageMessage(original.getSubimage(i, i, size, size),
            modified.getSubimage(i, i, size, size))
      }
    }
    case _ => log.error("Unknown message, could not start")
  }
}
