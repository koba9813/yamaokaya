package win.haya.yamaokaya

object YamaokayaFinder {

    private val registeredShops = listOf(
        // 北海道
        RegisteredShop(Coordinates(43.0143434, 144.3198345), "山岡家 釧路店", "北海道"),
        RegisteredShop(Coordinates(43.7981896, 143.8644379), "山岡家 北見店", "北海道"),
        RegisteredShop(Coordinates(42.9316136, 143.1932328), "山岡家 帯広店", "北海道"),
        RegisteredShop(Coordinates(43.8186571, 144.1164278), "山岡家 美幌店", "北海道"),
        RegisteredShop(Coordinates(42.8823215, 143.2006915), "山岡家 帯広南店", "北海道"),
        RegisteredShop(Coordinates(43.0133938, 144.4037959), "山岡家 釧路町店", "北海道"),
        RegisteredShop(Coordinates(43.9998096, 144.2841253), "山岡家 網走店", "北海道"),
        RegisteredShop(Coordinates(42.9721878, 143.2079996), "山岡家 音更店", "北海道"),
        RegisteredShop(Coordinates(43.5440312, 144.9864827), "山岡家 中標津店", "北海道"),
        RegisteredShop(Coordinates(43.8056026, 142.4256515), "山岡家 旭川永山店", "北海道"),
        RegisteredShop(Coordinates(45.3848388, 141.7064461), "山岡家 稚内店", "北海道"),
        RegisteredShop(Coordinates(43.7438051, 142.4087292), "山岡家 東光店", "北海道"),
        RegisteredShop(Coordinates(44.0646566, 143.5266339), "山岡家 遠軽店", "北海道"),
        RegisteredShop(Coordinates(44.1657267, 142.3951252), "山岡家 士別店", "北海道"),
        RegisteredShop(Coordinates(44.3676374, 143.3320663), "山岡家 紋別店", "北海道"),
        RegisteredShop(Coordinates(43.9348987, 141.6637967), "山岡家 留萌店", "北海道"),
        RegisteredShop(Coordinates(43.768393, 142.3250374), "山岡家 旭川神居店", "北海道"),
        RegisteredShop(Coordinates(42.8742036, 141.5839741), "山岡家 恵庭店", "北海道"),
        RegisteredShop(Coordinates(42.9734088, 141.567744), "山岡家 北広島店", "北海道"),
        RegisteredShop(Coordinates(43.2115483, 141.7839946), "山岡家 岩見沢店", "北海道"),
        RegisteredShop(Coordinates(43.1439194, 141.2765406), "山岡家 樽川店", "北海道"),
        RegisteredShop(Coordinates(43.5410208, 141.9221426), "山岡家 滝川店", "北海道"),
        RegisteredShop(Coordinates(42.8221253, 141.6572096), "山岡家 千歳店", "北海道"),
        RegisteredShop(Coordinates(43.1743917, 141.0600291), "山岡家 朝里店", "北海道"),
        RegisteredShop(Coordinates(43.3431415, 142.384644), "山岡家 富良野店", "北海道"),
        RegisteredShop(Coordinates(43.1924884, 140.8209115), "山岡家 余市店", "北海道"),
        RegisteredShop(Coordinates(43.108394, 141.5463879), "山岡家 新文京台店", "北海道"),
        RegisteredShop(Coordinates(43.0648918, 141.4877182), "山岡家 江別店", "北海道"),
        RegisteredShop(Coordinates(42.9895094, 141.4410939), "山岡家 羊ヶ丘通店", "北海道"),
        RegisteredShop(Coordinates(43.057967, 141.35621), "山岡家 南2条店", "北海道"),
        RegisteredShop(Coordinates(43.1300972, 141.2097702), "山岡家 手稲店", "北海道"),
        RegisteredShop(Coordinates(42.9615303, 141.2742336), "山岡家 藤野店", "北海道"),
        RegisteredShop(Coordinates(43.131467, 141.347034), "山岡家 太平店", "北海道"),
        RegisteredShop(Coordinates(43.0915742, 141.4186061), "山岡家 東雁来店", "北海道"),
        RegisteredShop(Coordinates(43.1039984, 141.3780466), "山岡家 新道店", "北海道"),
        RegisteredShop(Coordinates(43.0536157, 141.3535782), "山岡家 新すすきの店", "北海道"),
        RegisteredShop(Coordinates(43.0185341, 141.4077953), "山岡家 月寒店", "北海道"),
        RegisteredShop(Coordinates(43.0280475, 141.448433), "山岡家 大谷地店", "北海道"),
        RegisteredShop(Coordinates(43.0570279, 141.3517625), "山岡家 狸小路4丁目店", "北海道"),
        RegisteredShop(Coordinates(43.1065132, 141.2737397), "山岡家 新発寒店", "北海道"),
        RegisteredShop(Coordinates(43.0570238, 141.3516964), "味噌ラーメン山岡家 すすきの店", "北海道"),
        RegisteredShop(Coordinates(42.6167056, 141.5412865), "山岡家 苫小牧糸井店", "北海道"),
        RegisteredShop(Coordinates(42.3631776, 141.0568933), "山岡家 室蘭店", "北海道"),
        RegisteredShop(Coordinates(42.4696844, 140.8751831), "山岡家 伊達店", "北海道"),
        RegisteredShop(Coordinates(42.2571836, 140.2804866), "山岡家 八雲店", "北海道"),
        RegisteredShop(Coordinates(42.6471397, 141.6267641), "山岡家 苫小牧船見店", "北海道"),
        RegisteredShop(Coordinates(41.810416, 140.7691088), "山岡家 函館鍛冶店", "北海道"),
        RegisteredShop(Coordinates(42.890613099999996, 140.7519845), "山岡家 倶知安店", "北海道"),
        RegisteredShop(Coordinates(42.3413545, 142.3512688), "山岡家 新ひだか店", "北海道"),
        RegisteredShop(Coordinates(41.7886103, 140.7319824), "山岡家 函館万代店", "北海道"),
        RegisteredShop(Coordinates(42.6401468, 141.6131202), "味噌ラーメン山岡家 苫小牧店", "北海道"),
        // 青森県
        RegisteredShop(Coordinates(40.6262333, 140.4872742), "山岡家 弘前店", "青森県"),
        RegisteredShop(Coordinates(40.5087852, 141.5063306), "山岡家 八戸店", "青森県"),
        RegisteredShop(Coordinates(40.8284416, 140.7836557), "山岡家 青森東店", "青森県"),
        // 岩手県
        RegisteredShop(Coordinates(39.6804366, 141.1555256), "山岡家 岩手盛岡店", "岩手県"),
        RegisteredShop(Coordinates(39.7100725, 141.10535), "山岡家 盛岡インター店", "岩手県"),
        // 秋田県
        RegisteredShop(Coordinates(39.6867416, 140.1245236), "山岡家 秋田仁井田店", "秋田県"),
        RegisteredShop(Coordinates(39.7439016, 140.0896765), "山岡家 秋田寺内店", "秋田県"),
        // 山形県
        RegisteredShop(Coordinates(38.2212412, 140.3314594), "山岡家 山形青田店", "山形県"),
        RegisteredShop(Coordinates(38.2668201, 140.3123049), "山岡家 山形西田店", "山形県"),
        // 宮城県
        RegisteredShop(Coordinates(38.1198619, 140.874796), "山岡家 名取店", "宮城県"),
        RegisteredShop(Coordinates(38.3504563, 140.8747718), "山岡家 仙台泉区店", "宮城県"),
        RegisteredShop(Coordinates(38.2766083, 140.9904266), "山岡家 宮城野店", "宮城県"),
        RegisteredShop(Coordinates(38.2655383, 140.869467), "山岡家 定禅寺通店", "宮城県"),
        // 福島県
        RegisteredShop(Coordinates(36.9865093, 140.9025688), "山岡家 いわき店", "福島県"),
        RegisteredShop(Coordinates(37.7888247, 140.4602521), "山岡家 福島矢野目店", "福島県"),
        RegisteredShop(Coordinates(37.4260279, 140.344144), "山岡家 郡山店", "福島県"),
        RegisteredShop(Coordinates(37.2979485, 140.3679489), "山岡家 須賀川店", "福島県"),
        // 茨城県
        RegisteredShop(Coordinates(36.397650673276345, 140.50535377876412), "山岡家 ひたちなか店", "茨城県"),
        RegisteredShop(Coordinates(36.366183045752884, 140.48297166948979), "山岡家 水戸城南店", "茨城県"),
        RegisteredShop(Coordinates(36.537558996534784, 140.63643157503827), "山岡家 日立東金沢店", "茨城県"),
        RegisteredShop(Coordinates(36.537658129508564, 140.41588693877145), "山岡家 常陸大宮店", "茨城県"),
        RegisteredShop(Coordinates(36.377374656642665, 140.3608206027508), "山岡家 水戸内原店", "茨城県"),
        RegisteredShop(Coordinates(36.31298564606419, 140.44880778716356), "山岡家 水戸南店", "茨城県"),
        RegisteredShop(Coordinates(36.19229574055287, 140.2941730730318), "山岡家 石岡店", "茨城県"),
        RegisteredShop(Coordinates(36.13136315812844, 140.22564799696343), "山岡家 かすみがうら店", "茨城県"),
        RegisteredShop(Coordinates(36.085492839913535, 140.2060142271692), "山岡家 土浦店", "茨城県"),
        RegisteredShop(Coordinates(36.07663120071107, 140.1059249155306), "山岡家 つくば中央店", "茨城県"),
        RegisteredShop(Coordinates(36.04915550750586, 140.0848427526962), "山岡家 谷田部店", "茨城県"),
        RegisteredShop(Coordinates(35.99932258990889, 140.1533127114314), "山岡家 牛久店", "茨城県"),
        RegisteredShop(Coordinates(35.915208766063834, 140.63690426791038), "山岡家 神栖店", "茨城県"),
        RegisteredShop(Coordinates(36.2873896, 139.8835851), "山岡家 新結城店", "茨城県"),
        RegisteredShop(Coordinates(35.960660, 139.986108), "山岡家 守谷店", "茨城県"),
        RegisteredShop(Coordinates(36.348665, 140.052345), "山岡家 岩瀬店", "茨城県"),
        RegisteredShop(Coordinates(36.041131, 140.208893), "山岡家 阿見店", "茨城県"),
        RegisteredShop(Coordinates(36.040836, 140.2092861), "味噌ラーメン山岡家 阿見店", "茨城県"),
        // 栃木県
        RegisteredShop(Coordinates(36.2671246, 139.8271714), "山岡家 小山田間店", "栃木県"),
        RegisteredShop(Coordinates(36.2987547, 139.816505), "山岡家 小山駅南町店", "栃木県"),
        RegisteredShop(Coordinates(36.5974076, 139.8849839), "山岡家 宇都宮長岡店", "栃木県"),
        RegisteredShop(Coordinates(36.3078337, 139.4632362), "山岡家 足利店", "栃木県"),
        RegisteredShop(Coordinates(36.2959774, 139.6014027), "山岡家 佐野店", "栃木県"),
        RegisteredShop(Coordinates(36.5674311, 139.9932831), "山岡家 テクノポリスセンター店", "栃木県"),
        RegisteredShop(Coordinates(36.414249, 139.8980075), "山岡家 上三川店", "栃木県"),
        RegisteredShop(Coordinates(36.394488, 139.7299163), "山岡家 栃木店", "栃木県"),
        RegisteredShop(Coordinates(36.8424476, 139.9520549), "山岡家 大田原店", "栃木県"),
        RegisteredShop(Coordinates(36.5462495, 139.86438), "山岡家 鶴田店", "栃木県"),
        // 群馬県
        RegisteredShop(Coordinates(36.3432355, 139.3831653), "山岡家 太田店", "群馬県"),
        RegisteredShop(Coordinates(36.3342133, 138.9469017), "山岡家 高崎西店", "群馬県"),
        RegisteredShop(Coordinates(36.3290349, 139.1671695), "山岡家 伊勢崎宮子店", "群馬県"),
        RegisteredShop(Coordinates(36.2947057, 139.069438), "山岡家 高崎倉賀野店", "群馬県"),
        RegisteredShop(Coordinates(36.370088, 139.0234183), "山岡家 高崎中尾店", "群馬県"),
        RegisteredShop(Coordinates(36.2336972, 139.5431296), "山岡家 館林店", "群馬県"),
        RegisteredShop(Coordinates(36.266624, 139.3974419), "山岡家 大泉店", "群馬県"),
        RegisteredShop(Coordinates(36.3811427, 139.1047738), "山岡家 前橋野中店", "群馬県"),
        // 埼玉県
        RegisteredShop(Coordinates(35.9549972, 139.7263575), "山岡家 春日部店", "埼玉県"),
        RegisteredShop(Coordinates(35.9942227, 139.5764194), "山岡家 上尾店", "埼玉県"),
        RegisteredShop(Coordinates(35.9242078, 139.5834897), "山岡家 さいたま宮前店", "埼玉県"),
        RegisteredShop(Coordinates(35.9583397, 139.6563641), "山岡家 さいたま丸ヶ崎店", "埼玉県"),
        RegisteredShop(Coordinates(35.884252, 139.817204), "山岡家 越谷レイクタウン店", "埼玉県"),
        RegisteredShop(Coordinates(35.9686024, 139.4700418), "山岡家 川島店", "埼玉県"),
        RegisteredShop(Coordinates(36.1505235, 139.5386714), "山岡家 羽生店", "埼玉県"),
        RegisteredShop(Coordinates(35.9341125, 139.3902665), "山岡家 鶴ヶ島店", "埼玉県"),
        RegisteredShop(Coordinates(36.1848042, 139.3170081), "山岡家 深谷店", "埼玉県"),
        // 千葉県
        RegisteredShop(Coordinates(35.794417, 140.3192147), "山岡家 成田店", "千葉県"),
        RegisteredShop(Coordinates(35.7574649, 140.302621), "山岡家 成田飯仲店", "千葉県"),
        RegisteredShop(Coordinates(35.5543273, 140.3654611), "山岡家 東金店", "千葉県"),
        RegisteredShop(Coordinates(35.7135115, 140.2193346), "山岡家 千葉佐倉店", "千葉県"),
        RegisteredShop(Coordinates(35.5518, 140.1252878), "山岡家 千葉中央区店", "千葉県"),
        RegisteredShop(Coordinates(35.3952961, 139.9417582), "山岡家 木更津店", "千葉県"),
        RegisteredShop(Coordinates(35.684051, 140.128222), "山岡家 千葉花見川区店", "千葉県"),
        RegisteredShop(Coordinates(35.3248443, 139.9246205), "山岡家 君津店", "千葉県"),
        RegisteredShop(Coordinates(35.7713087, 140.0951749), "山岡家 八千代店", "千葉県"),
        RegisteredShop(Coordinates(35.6268812, 140.1266747), "山岡家 東千葉店", "千葉県"),
        RegisteredShop(Coordinates(35.6370453, 140.1667861), "山岡家 千葉若葉区店", "千葉県"),
        RegisteredShop(Coordinates(35.8937398, 139.9584636), "山岡家 柏店", "千葉県"),
        RegisteredShop(Coordinates(35.937545, 139.8901699), "山岡家 野田店", "千葉県"),
        RegisteredShop(Coordinates(35.8273997, 139.9362494), "山岡家 松戸北小金店", "千葉県"),
        // 東京都
        RegisteredShop(Coordinates(35.7650546, 139.3541323), "山岡家 瑞穂店", "東京都"),
        RegisteredShop(Coordinates(35.786256400000006, 139.30666800000003), "山岡家 青梅店", "東京都"),
        // 神奈川県
        RegisteredShop(Coordinates(35.488332, 139.3648977), "山岡家 厚木店", "神奈川県"),
        RegisteredShop(Coordinates(35.5629704, 139.3466083), "山岡家 相模原店", "神奈川県"),
        RegisteredShop(Coordinates(35.3563238, 139.357292), "山岡家 平塚店", "神奈川県"),
        RegisteredShop(Coordinates(35.3661472, 139.3619684), "山岡家 平塚田村店", "神奈川県"),
        // 新潟県
        RegisteredShop(Coordinates(37.1290648, 138.2473411), "山岡家 上越店", "新潟県"),
        RegisteredShop(Coordinates(37.4558185, 138.8110035), "山岡家 長岡堺店", "新潟県"),
        RegisteredShop(Coordinates(37.9020128, 139.0397342), "山岡家 新潟新和店", "新潟県"),
        RegisteredShop(Coordinates(37.8187252, 139.0195906), "山岡家 新潟白根大通店", "新潟県"),
        RegisteredShop(Coordinates(37.9340737, 139.0976758), "山岡家 新潟藤見店", "新潟県"),
        // 山梨県
        RegisteredShop(Coordinates(35.6428205, 138.6453223), "山岡家 笛吹店", "山梨県"),
        RegisteredShop(Coordinates(35.6606643, 138.5215455), "山岡家 山梨甲斐店", "山梨県"),
        RegisteredShop(Coordinates(35.4911451, 138.7444003), "山岡家 フォレスト河口湖店", "山梨県"),
        // 長野県
        RegisteredShop(Coordinates(36.6419115, 138.237275), "山岡家 長野南長池店", "長野県"),
        RegisteredShop(Coordinates(36.184297, 137.9628832), "山岡家 松本店", "長野県"),
        // 静岡県
        RegisteredShop(Coordinates(35.1351064, 138.6527572), "山岡家 富士店", "静岡県"),
        RegisteredShop(Coordinates(35.2435531, 138.6181984), "山岡家 富士宮店", "静岡県"),
        RegisteredShop(Coordinates(34.8780897, 138.3078201), "山岡家 焼津店", "静岡県"),
        RegisteredShop(Coordinates(34.7591795, 137.7526378), "山岡家 浜松有玉店", "静岡県"),
        RegisteredShop(Coordinates(34.7253463, 137.7792771), "山岡家 浜松薬師店", "静岡県"),
        RegisteredShop(Coordinates(34.6775816, 137.6807203), "山岡家 浜松南区店", "静岡県"),
        // 愛知県
        RegisteredShop(Coordinates(34.7791405, 137.3847552), "山岡家 豊橋下地店", "愛知県"),
        RegisteredShop(Coordinates(35.3253727, 136.9172402), "山岡家 大口店", "愛知県"),
        // 岐阜県
        RegisteredShop(Coordinates(35.3443973, 136.6220762), "山岡家 新大垣店", "岐阜県")
    )

    fun findNearest(current: Coordinates): ShopInfo? {
        var nearest: ShopInfo? = null

        for (shop in registeredShops) {
            val distance = calculateDistanceMeters(current, shop.coordinates)
            val bearing = calculateBearing(current, shop.coordinates)
            val candidate = ShopInfo(
                name = shop.name,
                coordinates = shop.coordinates,
                distanceMeters = distance,
                bearingDegrees = bearing
            )

            if (nearest == null || candidate.distanceMeters < nearest.distanceMeters) {
                nearest = candidate
            }
        }

        return nearest
    }

    fun getRegisteredShops(): List<RegisteredShop> = registeredShops

    fun getShopNames(): List<String> = registeredShops.map { it.name }

    fun getPrefectures(): List<String> = registeredShops.map { it.prefecture }.distinct()

    fun getShopsByPrefecture(prefecture: String): List<RegisteredShop> =
        registeredShops.filter { it.prefecture == prefecture }
}
