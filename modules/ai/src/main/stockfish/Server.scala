package lila.ai
package stockfish

import scala.concurrent.duration._

import akka.actor.{ Props, Actor, ActorRef, Kill }
import akka.pattern.{ ask, AskTimeoutException }
import play.api.libs.concurrent.Akka.system
import play.api.Play.current

import actorApi._
import chess.format.Forsyth
import chess.format.UciDump
import chess.Variant.Chess960
import lila.analyse.AnalysisMaker

private[ai] final class Server(queue: ActorRef, config: Config) {

  def play(pgn: String, initialFen: Option[String], level: Int): Fu[String] = {
    implicit val timeout = makeTimeout(config.playTimeout)
    UciDump(pgn, initialFen, initialFen.isDefined option Chess960).future flatMap { moves ⇒
      queue ? PlayReq(moves, initialFen map chess960Fen, level) mapTo
        manifest[Valid[String]] flatMap (_.future)
    }
  }

  def analyse(pgn: String, initialFen: Option[String]): Fu[AnalysisMaker] = {
    implicit val timeout = makeTimeout(config.analyseTimeout)
    UciDump(pgn, initialFen, initialFen.isDefined option Chess960).future flatMap { moves ⇒
      queue ? FullAnalReq(moves, initialFen map chess960Fen) mapTo
        manifest[Valid[AnalysisMaker]] flatMap (_.future)
    }
  }

  private def chess960Fen(fen: String) = (Forsyth << fen).fold(fen) { situation ⇒
    fen.replace("KQkq", situation.board.pieces.toList filter {
      case (_, piece) ⇒ piece is chess.Rook
    } sortBy {
      case (pos, _) ⇒ (pos.y, pos.x)
    } map {
      case (pos, piece) ⇒ piece.color.fold(pos.file.toUpperCase, pos.file)
    } mkString "")
  }
}
