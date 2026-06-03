package com.samsung_remote.tv

/** Where the connection to the TV currently stands. */
enum class Status { DISCONNECTED, CONNECTING, WAITING_APPROVAL, CONNECTED }

/**
 * The streaming shortcuts the app knows how to launch.
 *
 * This is a catalogue, not a fixed row: on connect every entry is probed against the TV and
 * only the ones actually installed are shown. So the same build gives each person the apps
 * on their own set, and nothing here needs to be configured.
 *
 * Ids are region- and rebrand-specific and several apps carry more than one across firmware
 * generations, hence a list of candidates per app. To add your own, find the id (a `GET` on
 * `http://<tv>:8001/api/v2/applications/<id>` answers 200 when installed) and add an entry.
 */
enum class TvApp(
    val label: String,
    private val keywords: List<String>,
    /** Tried in order against the TV's :8001 app endpoint; the first one it has wins. */
    val candidateIds: List<String>,
) {
    NETFLIX("Netflix", listOf("netflix"), listOf("3201907018807", "11101200001")),
    YOUTUBE("YouTube", listOf("youtube"), listOf("111299001912", "9Ur5IzDKqV.TizenYouTube")),
    PRIME("Prime", listOf("prime video", "prime"), listOf("3201910019365", "3201512006785")),

    // Verified on a 2024 set. This id is in no public list -- modern firmware refuses to
    // enumerate its apps, so it was found by searching Samsung's id space directly. It
    // decodes to 2017-11, years before the JioHotstar rebrand, because the id records when
    // the publisher first registered rather than when the brand launched.
    HOTSTAR("Hotstar", listOf("jiohotstar", "hotstar", "jio"), listOf("3201711015117")),

    SONYLIV("SonyLIV", listOf("sonyliv", "sony liv"), listOf("3201709014748")),
    DISNEY("Disney+", listOf("disney"), listOf("3201901017640", "3202009021709", "3202204027038")),
    APPLE_TV("Apple TV", listOf("apple tv"), listOf("3201807016597")),
    SPOTIFY("Spotify", listOf("spotify"), listOf("3201606009684")),
    TWITCH("Twitch", listOf("twitch"), listOf("3202203026841")),
    PLEX("Plex", listOf("plex"), listOf("3201512006963")),
    MAX("Max", listOf("max", "hbo"), listOf("3202301029760", "3201601007230")),
    HULU("Hulu", listOf("hulu"), listOf("3201601007625")),
    PEACOCK("Peacock", listOf("peacock"), listOf("3202006020991")),
    PARAMOUNT("Paramount+", listOf("paramount"), listOf("3202110025305", "3201710014981")),
    DISCOVERY("Discovery+", listOf("discovery"), listOf("3201803015944")),
    IPLAYER("iPlayer", listOf("iplayer"), listOf("3201601007670")),
    ITVX("ITVX", listOf("itv"), listOf("121299000089")),
    TUBI("Tubi", listOf("tubi"), listOf("3201504001965")),
    PLUTO("Pluto TV", listOf("pluto"), listOf("3201808016802")),
    RAKUTEN("Rakuten", listOf("rakuten"), listOf("3201511006428")),
    APPLE_MUSIC("Apple Music", listOf("apple music"), listOf("3201908019041")),
    TIDAL("TIDAL", listOf("tidal"), listOf("3201805016367")),
    DEEZER("Deezer", listOf("deezer"), listOf("3201608010191")),
    YT_KIDS("YT Kids", listOf("youtube kids"), listOf("3201611010983")),
    STEAM_LINK("Steam Link", listOf("steam"), listOf("3201702011851")),
    TV_PLUS("TV Plus", listOf("samsung tv plus", "tv plus"), listOf("3202405035480", "3202006020992")),
    BROWSER("Browser", listOf("browser", "internet"), listOf("3202010022079", "3201907018784"));

    fun matches(appName: String): Boolean {
        val lower = appName.lowercase()
        return keywords.any { lower.contains(it) }
    }
}
