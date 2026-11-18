package gatra

import korlibs.image.color.*
import korlibs.korge.view.Views
import kotlin.math.max
import kotlin.math.pow
import kotlin.random.Random

object StatScaler { fun scale(level: Int): Double = 1.15.pow(level - 1) }   // BaseStat x 1.15^(Level-1)

// ---------------- Bahasa ----------------
private val TBL = mapOf(
    "battle" to listOf("BERTARUNG", "BATTLE", "BATORU", "BATALLA"),
    "tap" to listOf("KETUK UNTUK MULAI", "TAP TO START", "TAPPU DE KAISHI", "TOCA PARA EMPEZAR"),
    "campaign" to listOf("KAMPANYE", "CAMPAIGN", "KYANPEEN", "CAMPANA"),
    "arena" to listOf("ARENA PvP", "ARENA PvP", "ARENA PvP", "ARENA PvP"),
    "raid" to listOf("BOSS RAID", "BOSS RAID", "BOSU REIDO", "JEFE RAID"),
    "shop" to listOf("TOKO", "SHOP", "SHOPPU", "TIENDA"),
    "heroes" to listOf("HERO", "HEROES", "HIIROO", "HEROES"),
    "equip" to listOf("PERLENGKAPAN", "EQUIPMENT", "SOUBI", "EQUIPO"),
    "guild" to listOf("GUILD", "GUILD", "GIRUDO", "GREMIO"),
    "quests" to listOf("MISI", "QUESTS", "KUESUTO", "MISIONES"),
    "settings" to listOf("PENGATURAN", "SETTINGS", "SETTEIGU", "AJUSTES"),
    "back" to listOf("KEMBALI", "BACK", "MODORU", "VOLVER"),
    "pass" to listOf("BATTLE PASS", "BATTLE PASS", "BATORU PASU", "PASE DE BATALLA"),
    "daily" to listOf("MISI HARIAN", "DAILY QUESTS", "DEIRII KUESUTO", "MISIONES DIARIAS"),
    "events" to listOf("EVENT", "EVENTS", "IBENTO", "EVENTOS"),
    "language" to listOf("BAHASA", "LANGUAGE", "GENGO", "IDIOMA"),
    "opacity" to listOf("TRANSPARANSI JOYSTICK", "JOYSTICK OPACITY", "JOISUTIKKU TOUMEIDO", "OPACIDAD JOYSTICK"),
    "joystick" to listOf("MODE JOYSTICK", "JOYSTICK MODE", "JOISUTIKKU MOODO", "MODO JOYSTICK"),
    "fixed" to listOf("TETAP", "FIXED", "KOTEI", "FIJO"),
    "floating" to listOf("MENGAMBANG", "FLOATING", "FURIITINGU", "FLOTANTE"),
    "volume" to listOf("VOLUME", "VOLUME", "BORYUMU", "VOLUMEN"),
    "res" to listOf("RESOLUSI", "RESOLUTION", "KAIZOUDO", "RESOLUCION"),
    "fps" to listOf("FPS", "FPS", "FPS", "FPS"),
    "coming" to listOf("SEGERA HADIR", "COMING SOON", "MOUSUGU", "PROXIMAMENTE"),
    "claim" to listOf("KLAIM", "CLAIM", "UKETORU", "RECLAMAR"),
    "unlock" to listOf("BUKA", "UNLOCK", "ANROKKU", "DESBLOQUEAR"),
    "select" to listOf("PILIH", "SELECT", "SENTAKU", "ELEGIR"),
    "equipped" to listOf("TERPASANG", "EQUIPPED", "SOUBICHUU", "EQUIPADO"),
    "win" to listOf("LEVEL SELESAI!", "LEVEL CLEAR!", "KURIA!", "NIVEL COMPLETADO!"),
    "lose" to listOf("KALAH", "DEFEATED", "HAIBOKU", "DERROTA"),
    "again" to listOf("MAIN LAGI", "AGAIN", "MOU ICHIDO", "OTRA VEZ"),
    "lobby" to listOf("LOBI", "LOBBY", "ROBII", "LOBBY"),
    "energy" to listOf("ENERGI KURANG", "NOT ENOUGH ENERGY", "ENAJII BUSOKU", "ENERGIA INSUFICIENTE"),
    "coins" to listOf("KOIN KURANG", "NOT ENOUGH COINS", "KOIN BUSOKU", "MONEDAS INSUFICIENTES"),
)
fun tr(key: String): String = TBL[key]?.getOrNull(Profile.lang) ?: key

// ---------------- Item (50) ----------------
enum class Rarity(val label: String, val color: RGBA, val mult: Double) {
    COMMON("Common", Colors["#b8b8b8"], 1.0), UNCOMMON("Uncommon", Colors["#4cd964"], 1.6),
    RARE("Rare", Colors["#4aa8ff"], 2.6), EPIC("Epic", Colors["#b24cff"], 4.0), LEGENDARY("Legendary", Colors["#ffb02e"], 6.5)
}
enum class Slot(val label: String) { WEAPON("Weapon"), ARMOR("Armor"), HELM("Helm"), ACCESSORY("Accessory"), CONSUMABLE("Items"), MATERIAL("Material") }

class Item(
    val id: Int, val name: String, val slot: Slot, val rarity: Rarity,
    val atk: Double = 0.0, val hp: Double = 0.0, val def: Double = 0.0, val spd: Double = 0.0,
    val crit: Double = 0.0, val heal: Double = 0.0
) {
    val price: Int get() = (300 * rarity.mult * (if (slot == Slot.MATERIAL) 0.5 else if (slot == Slot.CONSUMABLE) 0.4 else 1.0)).toInt()
    fun desc(): String {
        val p = mutableListOf<String>()
        if (atk > 0) p += "ATK+${atk.toInt()}"; if (hp > 0) p += "HP+${hp.toInt()}"
        if (def > 0) p += "DEF+${def.toInt()}"; if (spd > 0) p += "SPD+${spd.toInt()}"
        if (crit > 0) p += "CRIT+${(crit * 100).toInt()}%"; if (heal > 0) p += "HEAL ${(heal * 100).toInt()}%"
        return p.joinToString(" ")
    }
}

object Items {
    private val N = listOf(
        "Iron Sword", "Bamboo Staff", "Rusty Kunai", "Steel Katana", "Hunter Bow", "Twin Daggers",
        "Frost Blade", "Thunder Spear", "Flame Tanto", "Shadow Fang", "Storm Naginata", "Venom Edge",
        "Dragon Slayer", "Sun Reaper", "Gatra Blade",
        "Cloth Vest", "Leather Tunic", "Bamboo Guard", "Chain Shirt", "Ninja Garb", "Iron Plate",
        "Frost Mail", "Thunder Robe", "Ember Coat", "Shadow Cloak", "Storm Plate", "Venom Suit",
        "Dragon Scale Armor", "Sun Armor", "Mecha Plating",
        "Straw Hat", "Iron Helm", "Frost Crown", "Shadow Hood", "Dragon Helm",
        "Wooden Charm", "Silver Ring", "Jade Amulet", "Shadow Scarf", "Phoenix Pendant",
        "Small HP Potion", "Medium HP Potion", "Large HP Potion", "Elixir", "Phoenix Tear",
        "Iron Ore", "Spirit Herb", "Ice Crystal", "Shadow Essence", "Dragon Core"
    )
    private val R = Rarity.values()
    val all: List<Item> = (1..50).map { id ->
        val n = N[id - 1]
        when (id) {
            in 1..15 -> { val i = id - 1; val r = R[i / 3]; val v = 1 + 0.12 * (i % 3); Item(id, n, Slot.WEAPON, r, atk = 12 * r.mult * v, crit = 0.01 * (i % 3)) }
            in 16..30 -> { val i = id - 16; val r = R[i / 3]; val v = 1 + 0.12 * (i % 3); Item(id, n, Slot.ARMOR, r, hp = 140 * r.mult * v, def = 6 * r.mult * v) }
            in 31..35 -> { val r = R[id - 31]; Item(id, n, Slot.HELM, r, hp = 70 * r.mult, def = 3 * r.mult) }
            in 36..40 -> { val r = R[id - 36]; Item(id, n, Slot.ACCESSORY, r, atk = 4 * r.mult, spd = 5 * r.mult, crit = 0.015 * r.mult) }
            in 41..45 -> { val i = id - 41; Item(id, n, Slot.CONSUMABLE, R[i], heal = listOf(0.25, 0.4, 0.6, 0.8, 1.0)[i]) }
            else -> Item(id, n, Slot.MATERIAL, R[id - 46])
        }
    }
    fun byId(id: Int): Item = all[id - 1]
    private val commonPool = listOf(1, 2, 3, 4, 5, 6, 16, 17, 18, 19, 20, 21, 31, 32, 36, 37, 41, 41, 42, 46, 47)
    private val rarePool = listOf(7, 8, 9, 22, 23, 24, 33, 38)
    fun dropCommon(): Int = commonPool.random()
    fun dropBoss(): List<Int> = buildList {
        add(if (Random.nextDouble() < 0.03) listOf(13, 14, 15, 28, 29, 30, 35, 40).random() else rarePool.random())
        add(48); add(49); add(43)
    }
}

// ---------------- Hero (10) ----------------
class Skill(val name: String, val radius: Double, val mult: Double, val cd: Double, val stun: Double = 0.0, val heal: Double = 0.0, val burn: Double = 0.0)
class Hero(val name: String, val title: String, val look: Look, val hpMul: Double, val dmgMul: Double, val spdMul: Double,
           val rangeMul: Double, val skill: Skill, val price: Int)

object Heroes {
    private val blk = Colors["#0b0b10"]
    val all = listOf(
        Hero("Gatra Warrior", "Flame Swordsman", Look(blk, Colors["#ff7a2a"], Colors["#ff7a2a"], Colors["#ffb347"], Colors["#ff2a3a"], Colors["#6b4a35"]), 1.0, 1.0, 1.0, 1.0, Skill("Flame Slash", 230.0, 3.0, 3.0, burn = 3.0), 0),
        Hero("Shadow Blade", "Night Assassin", Look(Colors["#10163a"], Colors["#40c8ff"], Colors["#3aa0ff"], Colors["#8fd4ff"], Colors["#3aa0ff"]), 0.9, 0.95, 1.2, 1.0, Skill("Shadow Cut", 260.0, 2.6, 2.6), 0),
        Hero("Iron Fist", "Brawler", Look(Colors["#1a1a1f"], Colors["#c0c0c0"], Colors["#9aa0aa"], Colors["#d0d0d8"]), 1.4, 1.1, 0.9, 0.9, Skill("Quake Punch", 280.0, 2.2, 4.0, stun = 1.5), 6000),
        Hero("Storm Kunoichi", "Lightning Ninja", Look(Colors["#1a1430"], Colors["#ffe23a"], Colors["#ffe23a"], Colors["#fff08a"], Colors["#ffe23a"]), 0.9, 1.0, 1.25, 1.0, Skill("Chain Lightning", 320.0, 2.4, 3.2, stun = 0.8), 8000),
        Hero("Flame Ronin", "Wandering Samurai", Look(Colors["#1a0a0a"], Colors["#ff4020"], Colors["#ff3010"], Colors["#ff8a40"], Colors["#c01818"], Colors["#5a2a1a"]), 1.0, 1.25, 1.0, 1.0, Skill("Inferno Cut", 240.0, 3.4, 4.0, burn = 4.0), 10000),
        Hero("Frost Archer", "Ice Marksman", Look(Colors["#0e1a2a"], Colors["#aeeaff"], Colors["#7fd8ff"], Colors["#c8f0ff"], Colors["#7fd8ff"]), 0.95, 1.0, 1.05, 1.3, Skill("Ice Volley", 380.0, 2.2, 3.5, stun = 1.2), 12000),
        Hero("Thunder Monk", "Storm Priest", Look(Colors["#1a1030"], Colors["#ffd23a"], Colors["#b24cff"], Colors["#e0b0ff"]), 1.2, 1.0, 1.0, 1.0, Skill("Thunder Palm", 260.0, 2.5, 3.5, heal = 0.1), 14000),
        Hero("Venom Phantom", "Toxic Specter", Look(Colors["#0a1a10"], Colors["#7dff3a"], Colors["#3aff6a"], Colors["#a0ffb0"], Colors["#2fbf71"]), 0.95, 1.05, 1.1, 1.0, Skill("Toxic Cloud", 300.0, 1.6, 3.0, burn = 6.0), 16000),
        Hero("Blood Reaper", "Crimson Harvester", Look(Colors["#1a0508"], Colors["#ff2040"], Colors["#c01030"], Colors["#ff5070"], Colors["#c01030"], Colors["#400810"]), 0.85, 1.3, 1.05, 1.1, Skill("Blood Harvest", 240.0, 2.8, 4.0, heal = 0.25), 20000),
        Hero("Mecha Shogun", "Steel Colossus", Look(Colors["#2a3038"], Colors["#40ffe0"], Colors["#40c8ff"], Colors["#a0e8ff"]), 1.6, 1.2, 0.8, 1.0, Skill("Orbital Cannon", 400.0, 3.6, 5.0, stun = 1.0), 30000),
    )
}

// ---------------- Musuh & Level 1 ----------------
enum class EnemyType(val sc: Double, val hp: Double, val dmg: Double, val speed: Double, val range: Double, val cd: Double, val look: Look, val coins: Int) {
    STICKMAN(1.0, 140.0, 8.0, 95.0, 48.0, 1.3, Look(Colors["#0b0b10"], Colors["#ff3030"], Colors["#ff3030"], Colors["#9fb4c8"]), 20),
    NINJA(0.95, 110.0, 10.0, 175.0, 48.0, 0.9, Look(Colors["#1b2a5a"], Colors["#40c8ff"], Colors["#3a6aff"], Colors["#8fd4ff"], Colors["#40c8ff"]), 30),
    TRIBAL(1.1, 240.0, 14.0, 80.0, 56.0, 1.6, Look(Colors["#3a2418"], Colors["#ffd23a"], Colors["#c8873a"], Colors["#c8c8c8"], mask = true), 40)
}

object Level1Config {
    const val level = 1
    const val stageName = "STAGE 1-5"
    const val waveCount = 3
    const val squadsPerWave = 6      // 6 regu x 4 musuh = 24 musuh/wave, total 72 + 1 boss
    const val squadSize = 4
    const val squadInterval = 24.0   // detik antar regu -> total level sekitar 9-10 menit
    const val maxAlive = 14
    const val bossHp = 4200.0
    const val bossDmg = 30.0
    const val bossReward = 800
    fun bossCd(): Double = max(1.5, 4.0 * 0.97.pow(level - 1))
    fun pick(wave: Int, r: Double): EnemyType = when (wave) {
        0 -> if (r < 0.65) EnemyType.STICKMAN else EnemyType.TRIBAL
        1 -> if (r < 0.4) EnemyType.STICKMAN else if (r < 0.75) EnemyType.NINJA else EnemyType.TRIBAL
        else -> if (r < 0.3) EnemyType.STICKMAN else if (r < 0.65) EnemyType.NINJA else EnemyType.TRIBAL
    }
}

// ---------------- Profil / simpan data ----------------
class Stats(val hp: Double, val atk: Double, val def: Double, val spd: Double, val crit: Double)

object Profile {
    var coins = 25480; var gems = 1550; var energy = 85
    var lang = 0; var joyOpacity = 0.7; var joyFloating = false; var volume = 0.8; var resolution = 2; var fps = 60
    var hero = 0; val unlocked = mutableSetOf(0, 1)
    val inv = mutableMapOf(41 to 3, 1 to 1, 16 to 1)
    val equipped = mutableMapOf(Slot.WEAPON to 1, Slot.ARMOR to 16)
    val questProg = IntArray(5); val questClaimed = BooleanArray(5)
    var passXp = 0; var cleared = false

    val questText = listOf("Kalahkan 50 musuh", "Selesaikan 1 level", "Pakai skill 10x", "Capai combo 20", "Menang di Arena")
    val questTarget = listOf(50, 1, 10, 1, 1)
    val questCoins = listOf(1000, 0, 800, 600, 0)
    val questGems = listOf(0, 100, 0, 0, 50)

    fun addItem(id: Int, n: Int = 1) { inv[id] = (inv[id] ?: 0) + n }
    fun stats(): Stats {
        val h = Heroes.all[hero]
        var hp = 2000.0 * h.hpMul; var atk = 55.0 * h.dmgMul; var def = 0.0; var spd = 260.0 * h.spdMul; var crit = 0.15
        equipped.values.forEach { id -> val i = Items.byId(id); atk += i.atk; hp += i.hp; def += i.def; spd += i.spd; crit += i.crit }
        return Stats(hp, atk, def, spd.coerceAtMost(420.0), crit.coerceAtMost(0.8))
    }
    fun power(): Int { val s = stats(); return (s.atk * 80 + s.hp / 2 + s.def * 40).toInt() }

    fun save(v: Views) = runCatching {
        v.storage["gatra_save"] = listOf(
            coins, gems, energy, lang, joyOpacity, joyFloating, volume, resolution, fps, hero,
            unlocked.joinToString(","), inv.entries.joinToString(",") { "${it.key}:${it.value}" },
            equipped.entries.joinToString(",") { "${it.key.name}:${it.value}" },
            questProg.joinToString(","), questClaimed.joinToString(","), passXp, cleared
        ).joinToString(";")
    }

    fun load(v: Views) { runCatching {
        val p = (v.storage["gatra_save"] ?: return).split(";")
        coins = p[0].toInt(); gems = p[1].toInt(); energy = p[2].toInt(); lang = p[3].toInt()
        joyOpacity = p[4].toDouble(); joyFloating = p[5].toBoolean(); volume = p[6].toDouble()
        resolution = p[7].toInt(); fps = p[8].toInt(); hero = p[9].toInt()
        unlocked.clear(); p[10].split(",").filter { it.isNotEmpty() }.forEach { unlocked += it.toInt() }
        inv.clear(); p[11].split(",").filter { it.isNotEmpty() }.forEach { val kv = it.split(":"); inv[kv[0].toInt()] = kv[1].toInt() }
        equipped.clear(); p[12].split(",").filter { it.isNotEmpty() }.forEach { val kv = it.split(":"); equipped[Slot.valueOf(kv[0])] = kv[1].toInt() }
        p[13].split(",").forEachIndexed { i, s -> questProg[i] = s.toInt() }
        p[14].split(",").forEachIndexed { i, s -> questClaimed[i] = s.toBoolean() }
        passXp = p[15].toInt(); cleared = p[16].toBoolean()
    } }
}
