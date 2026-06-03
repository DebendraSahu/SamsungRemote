package com.samsung_remote.tv

/** Remote-control keys understood by Tizen TVs. */
object Keys {
    const val POWER = "KEY_POWER"
    const val UP = "KEY_UP"
    const val DOWN = "KEY_DOWN"
    const val LEFT = "KEY_LEFT"
    const val RIGHT = "KEY_RIGHT"
    const val ENTER = "KEY_ENTER"
    const val RETURN = "KEY_RETURN"
    const val HOME = "KEY_HOME"
    const val MENU = "KEY_MENU"
    const val VOL_UP = "KEY_VOLUP"
    const val VOL_DOWN = "KEY_VOLDOWN"
    const val MUTE = "KEY_MUTE"
    const val CH_UP = "KEY_CHUP"
    const val CH_DOWN = "KEY_CHDOWN"
    const val REWIND = "KEY_REWIND"
    const val FAST_FORWARD = "KEY_FF"

    /**
     * Opens the TV's input list, which is where a plugged-in USB drive appears.
     *
     * Tizen exposes no way to deep-link into USB contents — the media browser is not a
     * launchable app — so this is as direct as it gets: one key, then pick the drive.
     */
    const val SOURCE = "KEY_SOURCE"

    /** Single-key play/pause toggle. Present on 2016-and-newer Tizen sets. */
    const val PLAY_PAUSE = "KEY_PLAY_BACK"
}
