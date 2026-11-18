package gatra

import korlibs.image.color.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.time.*

private fun SContainer.header(title: String, color: RGBA = Colors["#ffb02e"]) {
    solidRect(1280.0, 720.0, Colors["#141830"])
    txt(title, 40.0, 20.0, 40.0, color)
    btn(tr("back"), 1060.0, 20.0, 180.0, 56.0, Colors["#3a4270"]) { Profile.save(views); sceneContainer.changeTo({ LobbyScene() }) }
    txt("COIN ${num(Profile.coins)}   GEM ${num(Profile.gems)}", 560.0, 30.0, 22.0, Colors["#ffd23a"])
}

// ===================== HERO =====================
class HeroesScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        header(tr("heroes"))
        val detail = container()
        var sel = Profile.hero
        var preview: Fighter? = null
        fun render() {
            detail.removeChildren()
            val h = Heroes.all[sel]
            panel(520.0, 100.0, 720.0, 590.0)
            preview = Fighter(h.look, 2.6, 100.0, 1.0, 1.0, 1.0, 1.0).xy(760.0, 470.0)
            detail.addChild(preview!!)
            detail.txt(h.name, 960.0, 120.0, 34.0, Colors["#ffd23a"]); detail.txt(h.title, 960.0, 165.0, 22.0)
            detail.txt("HP x${h.hpMul}   DMG x${h.dmgMul}", 960.0, 215.0, 22.0)
            detail.txt("SPD x${h.spdMul}  RANGE x${h.rangeMul}", 960.0, 250.0, 22.0)
            detail.txt("SKILL: ${h.skill.name}", 960.0, 300.0, 24.0, h.look.aura)
            detail.txt("x${h.skill.mult} dmg, radius ${h.skill.radius.toInt()}, CD ${h.skill.cd}s", 960.0, 335.0, 18.0)
            val ex = listOfNotNull(if (h.skill.stun > 0) "stun ${h.skill.stun}s" else null, if (h.skill.burn > 0) "burn ${h.skill.burn}s" else null, if (h.skill.heal > 0) "heal ${(h.skill.heal * 100).toInt()}%" else null)
            if (ex.isNotEmpty()) detail.txt(ex.joinToString(", "), 960.0, 362.0, 18.0, Colors["#ffb02e"])
            if (sel in Profile.unlocked) {
                if (Profile.hero == sel) detail.txt(tr("equipped"), 960.0, 600.0, 28.0, Colors["#3ddc4a"])
                else detail.btn(tr("select"), 960.0, 580.0, 240.0, 70.0, Colors["#2faa5a"]) { Profile.hero = sel; Profile.save(views); render() }
            } else detail.btn("${tr("unlock")} ${num(h.price)}", 960.0, 580.0, 240.0, 70.0, Colors["#e07b1f"], 22.0) {
                if (Profile.coins >= h.price) { Profile.coins -= h.price; Profile.unlocked += sel; Profile.save(views); render() } else popup(tr("coins")) }
        }
        Heroes.all.forEachIndexed { i, h ->
            val c = if (i in Profile.unlocked) Colors["#1b2142"] else Colors["#20202a"]
            btn(h.name + if (i in Profile.unlocked) "" else "  [LOCK]", 40.0, 100.0 + i * 58.0, 450.0, 52.0, c, 22.0) { sel = i; render() }
        }
        render()
        addFixedUpdater(60.timesPerSecond) { preview?.tick(1.0 / 60, false) }
    }
}

// ===================== EQUIPMENT =====================
class EquipmentScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        header(tr("equip"))
        var tab = Slot.WEAPON
        val list = container()
        fun render() {
            list.removeChildren()
            Items.all.filter { it.slot == tab }.forEachIndexed { i, item ->
                val n = Profile.inv[item.id] ?: 0
                val on = Profile.equipped[tab] == item.id
                val c = if (on) Colors["#1f4a2a"] else Colors["#1b2142"]
                val row = list.solidRect(760.0, 34.0, c).xy(40.0, 140.0 + i * 36.0)
                row.onClick {
                    if (n > 0 && tab != Slot.CONSUMABLE && tab != Slot.MATERIAL) { Profile.equipped[tab] = item.id; Profile.save(views); render() }
                }
                list.txt("${item.name}  x$n  ${item.desc()}${if (on) "  [E]" else ""}", 50.0, 144.0 + i * 36.0, 18.0, if (n > 0) item.rarity.color else Colors["#555566"])
            }
            val s = Profile.stats()
            list.panel(840.0, 140.0, 400.0, 260.0)
            list.txt("HP   ${s.hp.toInt()}", 860.0, 160.0, 26.0); list.txt("ATK  ${s.atk.toInt()}", 860.0, 200.0, 26.0)
            list.txt("DEF  ${s.def.toInt()}", 860.0, 240.0, 26.0); list.txt("SPD  ${s.spd.toInt()}", 860.0, 280.0, 26.0)
            list.txt("CRIT ${(s.crit * 100).toInt()}%", 860.0, 320.0, 26.0); list.txt("CP ${num(Profile.power())}", 860.0, 360.0, 26.0, Colors["#ffb02e"])
        }
        Slot.values().forEachIndexed { i, s -> btn(s.label, 40.0 + i * 190.0, 84.0, 180.0, 44.0, Colors["#2a3058"], 20.0) { tab = s; render() } }
        render()
    }
}

// ===================== SHOP =====================
class ShopScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        header(tr("shop"))
        val ids = listOf(41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 1, 2, 3, 16, 17, 18, 31, 36, 4, 19)
        val list = container()
        fun render() {
            list.removeChildren()
            ids.forEachIndexed { i, id ->
                val item = Items.byId(id)
                val x = 40.0 + (i / 10) * 610.0; val y = 100.0 + (i % 10) * 58.0
                list.btn("", x, y, 590.0, 52.0, Colors["#1b2142"]) {
                    if (Profile.coins >= item.price) { Profile.coins -= item.price; Profile.addItem(id); Profile.save(views); sceneContainer.changeTo({ ShopScene() }) } else popup(tr("coins"))
                }
                list.txt("${item.name}  ${item.desc()}  (x${Profile.inv[id] ?: 0})", x + 10, y + 12, 18.0, item.rarity.color)
                list.txt("${num(item.price)}", x + 500, y + 12, 18.0, Colors["#ffd23a"])
            }
        }
        render()
    }
}

// ===================== QUEST =====================
class QuestScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        header(tr("quests"))
        val list = container()
        fun render() {
            list.removeChildren()
            for (i in 0 until 5) {
                val y = 110.0 + i * 100.0
                list.panel(40.0, y, 1200.0, 84.0)
                val p = minOf(Profile.questProg[i], Profile.questTarget[i])
                list.txt(Profile.questText[i], 60.0, y + 12, 28.0)
                list.txt("$p/${Profile.questTarget[i]}   +${Profile.questCoins[i]} coin  +${Profile.questGems[i]} gem", 60.0, y + 50, 20.0, Colors["#ffb02e"])
                if (Profile.questClaimed[i]) list.txt("OK", 1090.0, y + 24, 30.0, Colors["#3ddc4a"])
                else if (p >= Profile.questTarget[i]) list.btn(tr("claim"), 1040.0, y + 12, 180.0, 60.0, Colors["#2faa5a"]) {
                    Profile.questClaimed[i] = true; Profile.coins += Profile.questCoins[i]; Profile.gems += Profile.questGems[i]; Profile.save(views); sceneContainer.changeTo({ QuestScene() }) }
            }
        }
        render()
    }
}

// ===================== SETTINGS =====================
class SettingsScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        solidRect(1280.0, 720.0, Colors["#141830"])
        val ui = container()
        fun save() { Profile.save(views) }
        fun render() {
            ui.removeChildren()
            ui.txt(tr("settings"), 40.0, 20.0, 40.0, Colors["#ffb02e"])
            ui.btn(tr("back"), 1060.0, 20.0, 180.0, 56.0, Colors["#3a4270"]) { save(); sceneContainer.changeTo({ LobbyScene() }) }
            ui.txt(tr("language"), 60.0, 120.0, 26.0)
            Lang4.forEachIndexed { i, n -> ui.btn(n, 500.0 + i * 170.0, 110.0, 150.0, 56.0, if (Profile.lang == i) Colors["#e07b1f"] else Colors["#2a3058"]) { Profile.lang = i; save(); render() } }
            ui.txt(tr("opacity"), 60.0, 220.0, 26.0)
            ui.btn("-", 500.0, 210.0, 70.0, 56.0, Colors["#2a3058"]) { Profile.joyOpacity = (Profile.joyOpacity - 0.1).coerceAtLeast(0.2); save(); render() }
            ui.txt("${(Profile.joyOpacity * 100).toInt()}%", 590.0, 222.0, 26.0)
            ui.btn("+", 700.0, 210.0, 70.0, 56.0, Colors["#2a3058"]) { Profile.joyOpacity = (Profile.joyOpacity + 0.1).coerceAtMost(1.0); save(); render() }
            ui.txt(tr("joystick"), 60.0, 320.0, 26.0)
            ui.btn(tr("fixed"), 500.0, 310.0, 220.0, 56.0, if (!Profile.joyFloating) Colors["#e07b1f"] else Colors["#2a3058"]) { Profile.joyFloating = false; save(); render() }
            ui.btn(tr("floating"), 740.0, 310.0, 220.0, 56.0, if (Profile.joyFloating) Colors["#e07b1f"] else Colors["#2a3058"]) { Profile.joyFloating = true; save(); render() }
            ui.txt(tr("volume"), 60.0, 420.0, 26.0)
            ui.btn("-", 500.0, 410.0, 70.0, 56.0, Colors["#2a3058"]) { Profile.volume = (Profile.volume - 0.1).coerceAtLeast(0.0); save(); render() }
            ui.txt("${(Profile.volume * 100).toInt()}%", 590.0, 422.0, 26.0)
            ui.btn("+", 700.0, 410.0, 70.0, 56.0, Colors["#2a3058"]) { Profile.volume = (Profile.volume + 0.1).coerceAtMost(1.0); save(); render() }
            ui.txt(tr("res"), 60.0, 520.0, 26.0)
            listOf("LOW", "MED", "HIGH").forEachIndexed { i, n -> ui.btn(n, 500.0 + i * 170.0, 510.0, 150.0, 56.0, if (Profile.resolution == i) Colors["#e07b1f"] else Colors["#2a3058"]) { Profile.resolution = i; save(); render() } }
            ui.txt(tr("fps"), 60.0, 620.0, 26.0)
            listOf(30, 60).forEachIndexed { i, n -> ui.btn("$n", 500.0 + i * 170.0, 610.0, 150.0, 56.0, if (Profile.fps == n) Colors["#e07b1f"] else Colors["#2a3058"]) { Profile.fps = n; save(); render() } }
        }
        render()
    }
    private val Lang4 = listOf("ID", "EN", "JP", "ES")
}
