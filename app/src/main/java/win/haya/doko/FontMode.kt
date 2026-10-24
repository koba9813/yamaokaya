package win.haya.doko

/**
 * アプリ全体のフォント。設定画面から切り替え、SharedPreferences に保存する。
 *
 * [storageKey] は保存用の識別子なので、変更すると既存ユーザーの設定が失われる点に注意。
 */
enum class FontMode(
    val storageKey: String,
    val displayName: String,
    val description: String
) {
    /** すっきりとしたスマートフォン向けフォント（デフォルト）。 */
    SMART_FONT_UI("smart_font_ui", "Smart Font UI", "すっきりとした、読みやすいフォント"),

    /** 従来からの丸みのあるフォント。 */
    ZEN_MARU("zen_maru_gothic", "丸ゴシック", "丸みのある、やわらかく読みやすいフォント");

    companion object {
        /** 保存された文字列から復元する。未保存・不正な値は [SMART_FONT_UI] にフォールバックする。 */
        fun fromStorageKey(key: String?): FontMode =
            entries.firstOrNull { it.storageKey == key } ?: SMART_FONT_UI
    }
}
