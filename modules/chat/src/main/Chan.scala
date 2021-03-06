package lila.chat

import play.api.i18n.Lang
import play.api.libs.json._

import lila.i18n.LangList
import lila.user.User

sealed trait Chan extends Ordered[Chan] {
  def typ: String
  def key: String
  def idOption: Option[String]
  def autoActive: Boolean

  val contextual: Boolean = false

  override def toString = key
}

sealed abstract class StaticChan(
    val typ: String,
    val name: String) extends Chan {

  val key = typ
  val idOption = none
  val autoActive = false

  def compare(other: Chan) = 1
}

sealed abstract class IdChan(
    val typ: String,
    val autoActive: Boolean) extends Chan {

  val id: String
  def key = s"${typ}_$id"
  def idOption = id.some

  def compare(other: Chan) = other match {
    case c: LangChan       ⇒ 1
    case c: ContextualChan ⇒ -1
    case c: IdChan ⇒ typ compare c.typ match {
      case 0 ⇒ id compare c.id
      case x ⇒ x
    }
    case _ ⇒ -1
  }
}

sealed trait ContextualChan { self: Chan ⇒
  override val contextual = true
  override def compare(other: Chan) = 1
}

sealed abstract class AutoActiveChan(typ: String, i: String)
    extends IdChan(typ, true) with ContextualChan {
  val id = i
}

object TvChan extends StaticChan(Chan.typ.tv, "TV") with ContextualChan

case class GameWatcherChan(i: String) extends AutoActiveChan(Chan.typ.gameWatcher, i)
case class GamePlayerChan(i: String) extends AutoActiveChan(Chan.typ.gamePlayer, i)
case class TournamentChan(i: String) extends AutoActiveChan(Chan.typ.tournament, i)

case class LangChan(lang: Lang) extends IdChan(Chan.typ.lang, false) {
  val id = lang.language
  override def compare(other: Chan) = other match {
    case c: LangChan ⇒ id compare c.id
    case _           ⇒ -1
  }
}
object LangChan {
  def apply(code: String): Option[LangChan] = LangList.exists(code) option LangChan(Lang(code))
  def apply(user: User): LangChan = LangChan(Lang(user.lang | "en"))
}

case class UserChan(u1: String, u2: String) extends IdChan("user", false) {
  val id = List(u1, u2).sorted mkString "/"
  def contains(userId: String) = u1 == userId || u2 == userId
}
object UserChan {
  def apply(id: String): Option[UserChan] = id.split("/") match {
    case Array(u1, u2) ⇒ UserChan(u1, u2).some
    case _             ⇒ none
  }
}
case class TeamChan(id: String) extends IdChan(Chan.typ.team, false)

case class NamedChan(chan: Chan, name: String) {

  def toJson = Json.obj(
    "key" -> chan.key,
    "type" -> chan.typ,
    "name" -> name)
}

object Chan {

  object typ {
    val lang = "lang"
    val tv = "tv"
    val gameWatcher = "gameWatcher"
    val gamePlayer = "gamePlayer"
    val tournament = "tournament"
    val user = "user"
    val team = "team"
  }

  def apply(typ: String, idOption: Option[String]): Option[Chan] = typ match {
    case Chan.typ.lang        ⇒ idOption flatMap LangChan.apply
    case Chan.typ.tv          ⇒ TvChan.some
    case Chan.typ.gameWatcher ⇒ idOption map GameWatcherChan
    case Chan.typ.gamePlayer  ⇒ idOption map GamePlayerChan
    case Chan.typ.tournament  ⇒ idOption map TournamentChan
    case Chan.typ.user        ⇒ idOption flatMap UserChan.apply
    case Chan.typ.team        ⇒ idOption map TeamChan.apply
    case lang                 ⇒ LangChan(lang)
  }

  def parse(str: String): Option[Chan] = str.split('_').toList match {
    case List(typ) ⇒ apply(typ, none)
    case typ :: id ⇒ apply(typ, id.mkString("_").some)
    case _         ⇒ None
  }

  def autoActive(str: String): Boolean = parse(str) ?? (_.autoActive)
  def contextual(str: String): Boolean = parse(str) ?? (_.contextual)
}

