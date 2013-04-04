package lila.team

import akka.actor.ActorRef
import com.typesafe.config.Config
import lila.common.PimpedConfig._

final class Env(
    config: Config,
    captcher: ActorRef,
    messenger: ActorRef,
    router: ActorRef,
    forum: ActorRef,
    db: lila.db.Env) {

  private val settings = new {
    val CollectionTeam = config getString "collection.team"
    val CollectionMember = config getString "collection.member"
    val CollectionRequest = config getString "collection.request"
    val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
    val PaginatorMaxUserPerPage = config getInt "paginator.max_user_per_page"
    val CacheCapacity = config getInt "cache.capacity"
  }
  import settings._

  lazy val forms = new DataForm(captcher)

  private[team] lazy val teamColl = db(CollectionTeam)
  private[team] lazy val requestColl = db(CollectionRequest)
  private[team] lazy val memberColl = db(CollectionMember)

  private[team] lazy val cached = new Cached(CacheCapacity)

  private lazy val notifier = new Notifier(
    messenger = messenger,
    router = router)
}

object Env {

  private def actors = lila.hub.Env.current.actor

  lazy val current = "[team] boot" describes new Env(
    config = lila.common.PlayApp loadConfig "bookmark",
    captcher = actors.captcher,
    messenger = actors.messenger,
    router = actors.router,
    forum = actors.forum,
    db = lila.db.Env.current)
}