package lila.user

import scala.concurrent.duration._

import chess.Speed
import org.joda.time.DateTime

case class User(
    id: String,
    username: String,
    rating: Int,
    progress: Int,
    perfs: Perfs,
    count: Count,
    artificial: Boolean = false,
    troll: Boolean = false,
    ipBan: Boolean = false,
    enabled: Boolean,
    roles: List[String],
    profile: Option[Profile] = None,
    engine: Boolean = false,
    toints: Int = 0,
    createdAt: DateTime,
    seenAt: Option[DateTime],
    lang: Option[String]) extends Ordered[User] {

  override def equals(other: Any) = other match {
    case u: User ⇒ id == u.id
    case _       ⇒ false
  }

  def compare(other: User) = id compare other.id

  def noTroll = !troll

  def canTeam = true

  def disabled = !enabled

  def usernameWithRating = s"$username ($rating)"

  def profileOrDefault = profile | Profile.default

  def hasGames = count.game > 0

  def countRated = count.rated

  private val recentDuration = 10.minutes
  def seenRecently: Boolean = timeNoSee < recentDuration

  def timeNoSee: Duration = seenAt.fold[Duration](Duration.Inf) { s ⇒
    (nowMillis - s.getMillis).millis
  }
}

object User {

  type ID = String

  val anonymous = "Anonymous"

  case class Active(user: User, lang: String)

  def normalize(username: String) = username.toLowerCase

  object BSONFields {
    val id = "_id"
    val username = "username"
    val rating = "rating"
    val progress = "progress"
    val artificial = "artificial"
    val perfs = "perfs"
    val count = "count"
    val troll = "troll"
    val ipBan = "ipBan"
    val enabled = "enabled"
    val roles = "roles"
    val profile = "profile"
    val engine = "engine"
    val toints = "toints"
    val createdAt = "createdAt"
    val seenAt = "seenAt"
    val lang = "lang"
    def glicko(perf: String) = s"$perfs.$perf.gl"
  }

  import lila.db.BSON
  import lila.db.BSON.BSONJodaDateTimeHandler

  private def userBSONHandler = new BSON[User] {

    import BSONFields._
    import reactivemongo.bson.BSONDocument
    implicit def countHandler = Count.tube.handler
    implicit def profileHandler = Profile.tube.handler
    implicit def perfsHandler = Perfs.tube.handler

    def reads(r: BSON.Reader): User = User(
      id = r str id,
      username = r str username,
      rating = r nInt rating,
      progress = r intD progress,
      perfs = r.getO[Perfs](perfs) | Perfs.default,
      count = r.get[Count](count),
      artificial = r boolD artificial,
      troll = r boolD troll,
      ipBan = r boolD ipBan,
      enabled = r bool enabled,
      roles = ~r.getO[List[String]](roles),
      profile = r.getO[Profile](profile),
      engine = r boolD engine,
      toints = r nIntD toints,
      createdAt = r date createdAt,
      seenAt = r dateO seenAt,
      lang = r strO lang)

    def writes(w: BSON.Writer, o: User) = BSONDocument(
      id -> o.id,
      username -> o.username,
      rating -> w.int(o.rating),
      progress -> w.int(o.progress),
      perfs -> o.perfs,
      count -> o.count,
      artificial -> w.boolO(o.artificial),
      troll -> w.boolO(o.troll),
      ipBan -> w.boolO(o.ipBan),
      enabled -> o.enabled,
      roles -> o.roles.some.filter(_.nonEmpty),
      profile -> o.profile,
      engine -> w.boolO(o.engine),
      toints -> w.intO(o.toints),
      createdAt -> o.createdAt,
      seenAt -> o.seenAt,
      lang -> o.lang)
  }

  private[user] lazy val tube = lila.db.BsTube(userBSONHandler)
}
