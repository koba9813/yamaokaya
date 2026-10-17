package win.haya.doko

/**
 * アプリ全体の配色テーマ。設定画面から切り替え、SharedPreferences に保存する。
 *
 * [storageKey] は保存用の識別子なので、変更すると既存ユーザーの設定が失われる点に注意。
 */
enum class ThemeMode(
    val storageKey: String,
    val displayName: String,
    val description: String
) {
    /** 端末の壁紙由来の配色を使う、従来からのテーマ（デフォルト）。 */
    DARK("dark", "ダーク", "従来の配色。端末のテーマに合わせた落ち着いたダークカラー"),

    /** 白を基調とした明るいテーマ。 */
    LIGHT("light", "ライト", "白を基調とした、明るく清潔感のあるライトカラー"),

    /** 山岡家ブランドの赤・黒・金を基調としたテーマ。 */
    YAMAOKAYA("yamaokaya", "山岡家", "赤・黒・金の山岡家ブランドカラー");

    /** ダーク系の配色かどうか（システムバーのアイコン色の切り替えに使用）。 */
    val isDark: Boolean
        get() = this != LIGHT

    companion object {
        /** 保存された文字列から復元する。未保存・不正な値は [DARK] にフォールバックする。 */
        fun fromStorageKey(key: String?): ThemeMode =
            entries.firstOrNull { it.storageKey == key } ?: DARK
    }
}
