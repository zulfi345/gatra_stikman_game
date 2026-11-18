package gatra

import korlibs.image.color.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.time.*
import kotlin.random.Random

class LobbyScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        solidRect(1280.0, 720.0, Colors["#141830"])
        circle(300.0, fill = Colors["#1d2550"]).xy(340.0, 120.0).also { it.alpha = 0.5 }
        circle(240.0, fill = Colors["#2a1d50"]).xy(700.0, 200.0).also { it.alpha = 0.4 }
        val hero = Heroes.all[Profile.hero]

        suspend fun start(mode: Mode, cost: Int) {
            if (Profile.energy < cost) { popup(tr("energy")); return }
            Profile.energy -= cost; Profile.save(views)
            sceneContainer.changeTo({ BattleScene(mode) })
        }
        suspend fun go(next: () -> Scene) { Profile.save(views); sceneContainer.changeTo({ next() }) }

        // --- Top kiri: profil ---
        panel(20.0, 14.0, 430.0, 100.0)
        circle(36.0, fill = hero.look.aura).xy(30.0, 26.0).also { it.alpha = 0.8 }
        circle(26.0, fill = Colors["#0b0b10"]).xy(40.0, 36.0)
        txt("GATRA_WARRIOR", 110.0, 20.0, 26.0)
        txt("CP ${num(Profile.power())}", 110.0, 52.0, 24.0, Colors["#ffb02e"])
        txt("LV.42", 40.0, 92.0, 16.0, Colors["#ffd23a"])
        solidRect(300.0, 12.0, Colors["#1a2a4a"]).xy(110.0, 88.0); solidRect(190.0, 12.0, Colors["#3ab0ff"]).xy(110.0, 88.0)

        // --- Judul ---
        txt("GATRA", 0.0, 10.0, 64.0, Colors["#ff9a3a"], 1280.0)
        txt("STICKMAN", 0.0, 70.0, 38.0, Colors["#e8d0b0"], 1280.0)

        // --- Top kanan: mata uang ---
        panel(820.0, 14.0, 190.0, 40.0); txt("COIN ${num(Profile.coins)}", 832.0, 22.0, 22.0, Colors["#ffd23a"])
        panel(1020.0, 14.0, 160.0, 40.0); txt("GEM ${num(Profile.gems)}", 1032.0, 22.0, 22.0, Colors["#c070ff"])
        panel(820.0, 62.0, 190.0, 40.0); txt("ENERGY ${Profile.energy}/100", 832.0, 70.0, 22.0, Colors["#ffe23a"])
        btn("+", 1012.0, 62.0, 40.0, 40.0, Colors["#e07b1f"], 24.0) {
            if (Profile.gems >= 20) { Profile.gems -= 20; Profile.energy = minOf(100, Profile.energy + 50); go { LobbyScene() } } else popup(tr("coins")) }
        btn("SET", 1190.0, 14.0, 70.0, 40.0, Colors["#2a3058"], 18.0) { go { SettingsScene() } }
        btn("MAIL", 1190.0, 62.0, 70.0, 40.0, Colors["#2a3058"], 18.0) { popup("Belum ada surat") }

        // --- Panel kiri ---
        val tier = Profile.passXp / 1000 + 1
        btn("", 20.0, 150.0, 360.0, 100.0, Colors["#1b2142"]) { popup("Battle Pass Tier $tier (${Profile.passXp % 1000}/1000 XP)\n" + tr("coming")) }
        txt(tr("pass"), 40.0, 160.0, 26.0); txt("TIER $tier   XP ${Profile.passXp % 1000}/1000", 40.0, 200.0, 20.0, Colors["#ffb02e"])
        val done = Profile.questClaimed.count { it }
        btn("", 20.0, 265.0, 360.0, 100.0, Colors["#1b2142"]) { go { QuestScene() } }
        txt(tr("daily"), 40.0, 275.0, 26.0); txt("$done/5", 40.0, 315.0, 22.0, Colors["#ffb02e"])
        btn("", 20.0, 380.0, 360.0, 100.0, Colors["#1b2142"]) { popup(tr("events") + ": " + tr("coming")) }
        txt(tr("events"), 40.0, 400.0, 26.0)

        // --- Hero di tengah ---
        solidRect(260.0, 40.0, Colors["#3a3f5c"]).xy(510.0, 520.0); solidRect(220.0, 20.0, Colors["#2a2e48"]).xy(530.0, 560.0)
        txt("${hero.name.uppercase()}", 0.0, 120.0, 26.0, Colors["#ffd23a"], 1280.0)
        txt(hero.title, 0.0, 152.0, 20.0, Colors["#c8c8d8"], 1280.0)
        val preview = Fighter(hero.look, 2.4, 100.0, 1.0, 1.0, 1.0, 1.0).xy(640.0, 525.0)
        var tt = 0.0
        addFixedUpdater(60.timesPerSecond) { tt += 1.0 / 60; preview.tick(1.0 / 60, false); if (tt > 3.0) { tt = 0.0; preview.swing() } }

        // --- Panel kanan ---
        btn(tr("campaign"), 900.0, 150.0, 360.0, 100.0, Colors["#1f4a5a"], 30.0) { start(Mode.CAMPAIGN, 5) }
        btn(tr("arena"), 900.0, 265.0, 360.0, 100.0, Colors["#4a2a5a"], 30.0) { start(Mode.ARENA, 5) }
        btn(tr("raid"), 900.0, 380.0, 360.0, 100.0, Colors["#2a3a4a"], 30.0) { start(Mode.RAID, 10) }

        // --- Nav bawah ---
        val nav = listOf<Pair<String, suspend () -> Unit>>(
            tr("shop") to { go { ShopScene() } }, tr("heroes") to { go { HeroesScene() } },
            tr("equip") to { go { EquipmentScene() } }, tr("guild") to { popup(tr("guild") + ": " + tr("coming")) },
            tr("quests") to { go { QuestScene() } })
        nav.forEachIndexed { i, (label, act) -> btn(label, 20.0 + i * 190.0, 620.0, 180.0, 85.0, Colors["#1b2142"], 22.0) { act() } }
        btn(tr("battle"), 990.0, 600.0, 270.0, 105.0, Colors["#d2451e"], 42.0) { start(Mode.CAMPAIGN, 5) }
        txt(tr("tap"), 990.0, 672.0, 16.0, Colors["#ffe0c0"], 270.0)
    }
}
