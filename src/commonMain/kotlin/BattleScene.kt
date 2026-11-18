package gatra

import korlibs.event.Key
import korlibs.image.color.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.time.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.random.Random

enum class Mode { CAMPAIGN, ARENA, RAID }
class Floater(val t: Text, var life: Double)
class Part(val v: View, var vx: Double, var vy: Double, var life: Double)

class BattleScene(private val mode: Mode = Mode.CAMPAIGN) : Scene() {
    override suspend fun SContainer.sceneMain() {
        val cfg = Level1Config
        val W = 1280.0; val H = 720.0; val footY = 650.0
        val statScale = StatScaler.scale(cfg.level)
        val st = Profile.stats(); val hero = Heroes.all[Profile.hero]; val sk = hero.skill
        val fps = Profile.fps; val dt = 1.0 / fps

        // ---------- Latar: kuil malam ----------
        solidRect(W, H, Colors["#12152a"])
        circle(80.0, fill = Colors["#e8edff"]).xy(930.0, 50.0).also { it.alpha = 0.9 }
        for (i in 0 until 7) solidRect(120.0, 60.0 + (i % 3) * 40.0, Colors["#1a1e38"]).xy(i * 190.0, 430.0 - (i % 3) * 40.0)
        solidRect(16.0, 230.0, Colors["#7a1f2a"]).xy(560.0, 360.0); solidRect(16.0, 230.0, Colors["#7a1f2a"]).xy(760.0, 360.0)
        solidRect(280.0, 18.0, Colors["#7a1f2a"]).xy(530.0, 350.0); solidRect(240.0, 12.0, Colors["#7a1f2a"]).xy(550.0, 392.0)
        solidRect(W, H - 600.0, Colors["#2b2e42"]).xy(0.0, 600.0)
        for (i in 0 until 6) solidRect(W, 3.0, Colors["#1c1f30"]).xy(0.0, 625.0 + i * 18.0)
        solidRect(W, 6.0, Colors["#ff7a2a"]).xy(0.0, 600.0)
        val flames = mutableListOf<Circle>()
        for (tx in listOf(60.0, 1210.0)) { solidRect(18.0, 60.0, Colors["#3a3d55"]).xy(tx, 540.0); flames += circle(16.0, fill = Colors["#ff8a1f"]).xy(tx - 7, 505.0) }

        val world = container()
        val hud = container()

        // ---------- Pemain ----------
        val player = Fighter(hero.look, 1.0, st.hp, st.atk, st.spd, 95.0 * hero.rangeMul, 0.28, isPlayer = true)
        world.addChild(player); player.xy(300.0, footY)
        val enemies = mutableListOf<Fighter>()
        val floaters = mutableListOf<Floater>()
        val parts = mutableListOf<Part>()

        // ---------- HUD ----------
        hud.txt("GATRA_WARRIOR  LV.42   ${hero.name}", 130.0, 10.0, 20.0)
        hud.solidRect(360.0, 22.0, Colors["#1d2a1d"]).xy(130.0, 42.0)
        val hpFill = hud.solidRect(360.0, 22.0, Colors["#3ddc4a"]).xy(130.0, 42.0)
        val stageTxt = hud.txt("", 520.0, 8.0, 28.0)
        val coinTxt = hud.txt("COIN ${num(Profile.coins)}", 1010.0, 10.0, 22.0, Colors["#ffd23a"])
        val comboTxt = hud.txt("", 40.0, 140.0, 46.0, Colors["#ffb02e"])
        val bannerTxt = hud.txt("", 470.0, 250.0, 60.0)
        val bossLbl = hud.txt("", 700.0, 62.0, 20.0, Colors["#ffb02e"])
        val bossBg = hud.solidRect(520.0, 16.0, Colors["#3a1010"]).xy(700.0, 90.0)
        val bossFill = hud.solidRect(520.0, 16.0, Colors["#ff5a1f"]).xy(700.0, 90.0)
        bossLbl.visible = false; bossBg.visible = false; bossFill.visible = false
        val telegraph = world.solidRect(640.0, 36.0, Colors["#ff2020"]).xy(0.0, 590.0).also { it.alpha = 0.3; it.visible = false }

        // ---------- State ----------
        var earned = 0; val drops = mutableListOf<Int>()
        var combo = 0; var comboTimer = 0.0
        var skillCd = 0.0; var dashCd = 0.0; var iframes = 0.0; var potionCd = 0.0
        var wave = 0; var squadsDone = 0; var squadTimer = 1.0; var waveDelay = 2.0
        var bossSpawned = false; var boss: Fighter? = null
        var bossMode = 0; var bossModeT = 0.0; var bossCd = 3.0; var chargeDir = 1; var chargeHit = false; var summoned = false
        var rival: Fighter? = null; var rivalBurst = 0.0; var rivalBurstCd = 3.0; var roundT = 84.0
        var state = 0; var paused = false; var shake = 0.0
        var axis = 0.0; var padAtk = false; var reqSkill = false; var reqDash = false; var reqPotion = false

        fun floatText(s: String, x: Double, y: Double, c: RGBA, size: Double = 26.0) {
            floaters += Floater(world.text(s, size, c).xy(x - 30, y), 0.7)
        }
        fun burst(x: Double, y: Double, c: RGBA, n: Int) {
            repeat(n) {
                val v = world.circle(3.0 + Random.nextDouble() * 3, fill = c).xy(x, y)
                parts += Part(v, Random.nextDouble(-220.0, 220.0), Random.nextDouble(-320.0, -60.0), 0.5)
            }
        }
        fun hurtPlayer(a: Double) {
            if (iframes > 0 || state != 0) return
            val real = a * 100.0 / (100.0 + st.def)
            player.hurt(real); iframes = 0.3; shake = max(shake, 0.12)
            floatText("-${real.toInt()}", player.x, player.y - 150, Colors["#ff4040"])
        }
        fun hitEnemy(e: Fighter, mult: Double, skillHit: Boolean) {
            val crit = Random.nextDouble() < st.crit
            val d = player.dmg * mult * (if (crit) 2.2 else 1.0)
            e.hurt(d); combo++; comboTimer = 2.5
            if (combo >= 20) Profile.questProg[3] = 1
            if (!e.isBoss) e.x += player.facing * 14.0
            if (crit) shake = max(shake, 0.25)
            burst(e.x, e.y - 60 * e.sc, if (crit) Colors["#ffd23a"] else hero.look.aura, if (crit) 10 else 5)
            floatText(if (crit) "CRIT! ${d.toInt()}" else "-${d.toInt()}", e.x, e.y - 150 * e.sc, if (crit) Colors["#ffd23a"] else Colors["#ff6a3a"], if (crit) 34.0 else 26.0)
            if (skillHit) {
                e.stun = max(e.stun, if (e.isBoss) sk.stun * 0.3 else sk.stun); e.burn = max(e.burn, sk.burn)
                if (sk.heal > 0) player.hp = min(player.maxHp, player.hp + d * sk.heal)
            }
        }
        fun spawn(t: EnemyType, i: Int = 0) {
            val e = Fighter(t.look, t.sc, t.hp * statScale, t.dmg * statScale, t.speed, t.range, t.cd, t.coins)
            world.addChild(e)
            e.xy(if (Random.nextBoolean()) -40.0 - i * 70 else W + 40.0 + i * 70, footY)
            enemies += e
        }
        fun spawnBoss() {
            val hpMul = if (mode == Mode.RAID) 2.0 else 1.0
            val b = Fighter(Look(Colors["#3a2418"], Colors["#ffd23a"], Colors["#2fbf71"], Colors["#e0402e"], mask = true),
                2.1, cfg.bossHp * hpMul * statScale, cfg.bossDmg * statScale, 70.0, 120.0, 1.4, cfg.bossReward, isBoss = true)
            world.addChild(b); b.xy(W - 150.0, footY); enemies += b; boss = b
            bossLbl.text = "TRIBAL MASK CHIEFTAIN"; bossLbl.visible = true; bossBg.visible = true; bossFill.visible = true
            bannerTxt.text = "BOSS!"; waveDelay = 2.0
        }

        fun finish(win: Boolean) {
            if (state != 0) return
            state = if (win) 1 else 2
            var bonus = if (!win) 100 else when (mode) { Mode.CAMPAIGN -> 800; Mode.ARENA -> 800; Mode.RAID -> 1500 }
            if (win && mode != Mode.ARENA) { drops += Items.dropBoss() }
            if (win && mode == Mode.CAMPAIGN) { Profile.cleared = true; Profile.questProg[1] = 1; Profile.passXp += 500 }
            if (win && mode == Mode.ARENA) Profile.questProg[4] = 1
            Profile.coins += earned + bonus
            drops.forEach { Profile.addItem(it) }
            Profile.save(views)
            hud.solidRect(W, H, Colors["#000000"]).also { it.alpha = 0.65 }.onClick { }
            hud.txt(if (win) tr("win") else tr("lose"), 0.0, 60.0, 70.0, if (win) Colors["#ffd23a"] else Colors["#ff4040"], W)
            hud.txt("+${num(earned + bonus)} COIN", 0.0, 160.0, 34.0, Colors["#ffd23a"], W)
            drops.take(7).forEachIndexed { i, id -> val it2 = Items.byId(id); hud.txt(it2.name + "  [" + it2.rarity.label + "]", 440.0, 215.0 + i * 36.0, 26.0, it2.rarity.color) }
            hud.btn(tr("again"), 360.0, 530.0, 260.0, 70.0, Colors["#e07b1f"]) {
                if (Profile.energy >= 5) { Profile.energy -= 5; sceneContainer.changeTo({ BattleScene(mode) }) } else popup(tr("energy")) }
            hud.btn(tr("lobby"), 660.0, 530.0, 260.0, 70.0, Colors["#3a4270"]) { sceneContainer.changeTo({ LobbyScene() }) }
        }

        // ---------- Mode ----------
        stageTxt.text = "${cfg.stageName}  WAVE 1/3"
        if (mode == Mode.RAID) { bossSpawned = true; waveDelay = 0.0; spawnBoss(); stageTxt.text = "BOSS RAID" }
        if (mode == Mode.ARENA) {
            waveDelay = 0.0; bossSpawned = true
            val r = Fighter(Look(Colors["#14102a"], Colors["#c04bff"], Colors["#8a3cff"], Colors["#b57bff"], Colors["#b57bff"], Colors["#3a1a5a"]),
                1.0, 2300.0, 26.0, 200.0, 95.0, 0.75, showBar = false)
            world.addChild(r); r.xy(W - 300.0, footY); r.face(-1); enemies += r; rival = r
            bossLbl.text = "SHADOW_BLADE  LV.43"; bossLbl.visible = true; bossBg.visible = true; bossFill.visible = true
            bannerTxt.text = "FIGHT!"; waveDelay = 1.2
        }

        // ---------- Joystick virtual ----------
        val joyArea = hud.solidRect(640.0, 720.0, Colors.WHITE).also { it.alpha = 0.01 }
        val joyBase = hud.circle(80.0, fill = Colors["#40c8ff"]).also { it.alpha = Profile.joyOpacity * 0.5 }
        val joyKnob = hud.circle(34.0, fill = Colors["#bfeaff"]).also { it.alpha = Profile.joyOpacity }
        var jcx = 170.0; var jcy = 560.0
        fun placeJoy() { joyBase.xy(jcx - 80, jcy - 80); joyKnob.xy(jcx + axis * 70 - 34, jcy - 34) }
        placeJoy()
        if (Profile.joyFloating) { joyBase.visible = false; joyKnob.visible = false }
        joyArea.onDown {
            if (Profile.joyFloating) { jcx = joyArea.localMouseX(views); jcy = joyArea.localMouseY(views); joyBase.visible = true; joyKnob.visible = true }
            axis = ((joyArea.localMouseX(views) - jcx) / 70.0).coerceIn(-1.0, 1.0); placeJoy()
        }
        joyArea.onMove { axis = ((joyArea.localMouseX(views) - jcx) / 70.0).coerceIn(-1.0, 1.0); placeJoy() }
        val joyUp = { axis = 0.0; placeJoy(); if (Profile.joyFloating) { joyBase.visible = false; joyKnob.visible = false } }
        joyArea.onUp { joyUp() }; joyArea.onUpOutside { joyUp() }

        fun round(label: String, cx: Double, cy: Double, r: Double, color: RGBA, down: () -> Unit, up: () -> Unit = {}): Text {
            val c = hud.circle(r, fill = color).xy(cx - r, cy - r).also { it.alpha = Profile.joyOpacity }
            c.onDown { down() }; c.onUp { up() }; c.onUpOutside { up() }
            return hud.txt(label, cx - r * 0.7, cy - 12, 22.0, Colors.WHITE, r * 1.4)
        }
        round("ATK", 1170.0, 610.0, 70.0, Colors["#d2451e"], { padAtk = true }, { padAtk = false })
        val skillLbl = round("SKILL", 1030.0, 650.0, 46.0, Colors["#e07b1f"], { reqSkill = true })
        round("DASH", 1060.0, 530.0, 40.0, Colors["#2e8bd6"], { reqDash = true })
        val potLbl = round("HP", 930.0, 660.0, 36.0, Colors["#2faa5a"], { reqPotion = true })
        val pauseBtn = hud.circle(22.0, fill = Colors["#2a2f4a"]).xy(1215.0, 60.0)
        hud.txt("II", 1224.0, 70.0, 20.0)
        pauseBtn.onClick { paused = !paused }

        // ---------- Loop ----------
        addFixedUpdater(fps.timesPerSecond) {
            val d = dt
            val keys = views.input.keys
            flames.forEach { it.alpha = 0.7 + Random.nextDouble() * 0.3 }
            // partikel, floater, shake
            parts.toList().forEach { p -> p.life -= d; p.vy += 900 * d; p.v.x += p.vx * d; p.v.y += p.vy * d; p.v.alpha = (p.life * 2).coerceIn(0.0, 1.0); if (p.life <= 0) { p.v.removeFromParent(); parts.remove(p) } }
            floaters.toList().forEach { f -> f.life -= d; f.t.y -= 50 * d; if (f.life <= 0) { f.t.removeFromParent(); floaters.remove(f) } }
            if (shake > 0) { shake -= d; world.x = (Random.nextDouble() - 0.5) * shake * 40; world.y = (Random.nextDouble() - 0.5) * shake * 40 } else { world.x = 0.0; world.y = 0.0 }
            if (state != 0 || paused) return@addFixedUpdater

            // Input
            val kx = (if (keys.pressing(Key.LEFT) || keys.pressing(Key.A)) -1.0 else 0.0) + (if (keys.pressing(Key.RIGHT) || keys.pressing(Key.D)) 1.0 else 0.0)
            val mvAxis = if (abs(kx) > 0) kx else if (abs(axis) > 0.2) axis else 0.0
            val moving = abs(mvAxis) > 0.0
            if (moving) { player.x = (player.x + mvAxis * player.moveSpd * d).coerceIn(30.0, W - 30.0); player.face(sign(mvAxis).toInt()) }
            if (keys.pressing(Key.L)) reqSkill = true
            if (keys.pressing(Key.K)) reqDash = true
            if (keys.pressing(Key.H)) reqPotion = true
            val atk = padAtk || keys.pressing(Key.J) || keys.pressing(Key.SPACE)
            skillCd -= d; dashCd -= d; iframes -= d; potionCd -= d

            if (atk && player.atkTimer <= 0) {
                player.atkTimer = player.atkCd; player.swing()
                enemies.toList().forEach { e -> val dx = (e.x - player.x) * player.facing; if (dx > -20 && dx < player.range + 30 * e.sc) hitEnemy(e, 1.0, false) }
            }
            if (reqSkill && skillCd <= 0) {
                skillCd = sk.cd; player.swing(); Profile.questProg[2]++; shake = max(shake, 0.2)
                enemies.toList().forEach { e -> if (abs(e.x - player.x) < sk.radius) hitEnemy(e, sk.mult, true) }
                burst(player.x, player.y - 50, hero.look.aura, 14)
                floatText(sk.name.uppercase() + "!", player.x, player.y - 170, hero.look.aura, 30.0)
            }
            if (reqDash && dashCd <= 0) { dashCd = 2.0; iframes = 0.3; player.x = (player.x + player.facing * 230.0).coerceIn(30.0, W - 30.0); burst(player.x, player.y - 40, Colors.WHITE, 6) }
            if (reqPotion && potionCd <= 0) {
                val id = (41..45).firstOrNull { (Profile.inv[it] ?: 0) > 0 }
                if (id != null) { Profile.inv[id] = Profile.inv[id]!! - 1; potionCd = 1.5
                    val h = Items.byId(id).heal * player.maxHp; player.hp = min(player.maxHp, player.hp + h); floatText("+${h.toInt()}", player.x, player.y - 150, Colors["#3ddc4a"]) }
            }
            reqSkill = false; reqDash = false; reqPotion = false
            player.tick(d, moving)

            // Alur wave (kampanye)
            if (mode == Mode.CAMPAIGN || waveDelay > 0) {
                if (waveDelay > 0) { waveDelay -= d; if (waveDelay <= 0) bannerTxt.text = "" }
                else if (mode == Mode.CAMPAIGN && !bossSpawned) {
                    if (squadsDone < cfg.squadsPerWave) {
                        squadTimer -= d
                        if (squadTimer <= 0 && enemies.size <= cfg.maxAlive) { repeat(cfg.squadSize) { spawn(cfg.pick(wave, Random.nextDouble()), it) }; squadsDone++; squadTimer = cfg.squadInterval }
                    } else if (enemies.isEmpty()) {
                        if (wave < cfg.waveCount - 1) { wave++; squadsDone = 0; squadTimer = 1.0; waveDelay = 3.0; bannerTxt.text = "WAVE ${wave + 1}/3"; stageTxt.text = "${cfg.stageName}  WAVE ${wave + 1}/3" }
                        else { bossSpawned = true; spawnBoss() }
                    }
                }
            }

            // AI musuh
            enemies.toList().forEach { e ->
                e.tick(d, true)
                if (e.burn > 0) { e.burn -= d; e.burnAcc += d; if (e.burnAcc >= 0.5) { e.burnAcc = 0.0; e.hurt(player.dmg * 0.3); floatText("${(player.dmg * 0.3).toInt()}", e.x, e.y - 150 * e.sc, Colors["#ff8a1f"], 20.0) } }
                if (e.stun > 0) { e.stun -= d; return@forEach }
                val dx = player.x - e.x; val dir = sign(dx).toInt()
                if (e === rival) {
                    e.face(dir); rivalBurstCd -= d
                    if (rivalBurst > 0) rivalBurst -= d
                    if (rivalBurstCd <= 0) { rivalBurstCd = 4.0; rivalBurst = 0.5 }
                    val sp = e.moveSpd * (if (rivalBurst > 0) 3.0 else 1.0)
                    if (abs(dx) > e.stopDist) e.x += dir * sp * d
                    else if (e.atkTimer <= 0) { e.atkTimer = e.atkCd; e.swing(); if (abs(dx) <= e.range + 10) hurtPlayer(e.dmg) }
                    bossFill.width = 520.0 * (e.hp / e.maxHp).coerceIn(0.0, 1.0)
                } else if (!e.isBoss) {
                    e.face(dir)
                    if (abs(dx) > e.stopDist) e.x += dir * e.moveSpd * d
                    else if (e.atkTimer <= 0) { e.atkTimer = e.atkCd; e.swing(); if (abs(dx) <= e.range + 10) hurtPlayer(e.dmg) }
                } else {
                    when (bossMode) {
                        0 -> {
                            e.face(dir); bossCd -= d
                            if (abs(dx) > 110.0) e.x += dir * e.moveSpd * d
                            else if (e.atkTimer <= 0) { e.atkTimer = e.atkCd; e.swing(); hurtPlayer(e.dmg) }
                            if (bossCd <= 0) {
                                if (Random.nextBoolean()) { bossMode = 1; bossModeT = 1.0; telegraph.visible = true; telegraph.x = e.x - 320 }
                                else { bossMode = 2; bossModeT = 0.9; chargeDir = if (dir == 0) 1 else dir; chargeHit = false; e.face(chargeDir) }
                            }
                        }
                        1 -> { bossModeT -= d; telegraph.alpha = 0.2 + 0.3 * (1 - bossModeT.coerceIn(0.0, 1.0))
                            if (bossModeT <= 0) { telegraph.visible = false; shake = 0.5
                                if (abs(player.x - e.x) < 320) hurtPlayer(e.dmg * 1.8)
                                burst(e.x, footY - 20, Colors["#c8873a"], 16); e.swing(); bossMode = 0; bossCd = cfg.bossCd() } }
                        2 -> { bossModeT -= d; e.x = (e.x + chargeDir * 520.0 * d).coerceIn(60.0, W - 60.0)
                            if (!chargeHit && abs(player.x - e.x) < 70) { chargeHit = true; hurtPlayer(e.dmg * 1.4) }
                            if (bossModeT <= 0) { bossMode = 0; bossCd = cfg.bossCd() } }
                    }
                    if (!summoned && e.hp <= e.maxHp * 0.5) { summoned = true; repeat(3) { spawn(EnemyType.TRIBAL, it) }; floatText("MINION!", e.x, e.y - 280, Colors["#2fbf71"], 32.0) }
                    bossFill.width = 520.0 * (e.hp / e.maxHp).coerceIn(0.0, 1.0)
                }
            }

            // Musuh mati
            enemies.toList().forEach { e ->
                if (e.hp <= 0) {
                    earned += e.coins; e.removeFromParent(); enemies.remove(e); burst(e.x, e.y - 50, e.look.aura, 12)
                    Profile.questProg[0]++; Profile.passXp += 25
                    if (!e.isBoss && e !== rival && Random.nextDouble() < 0.08) drops += Items.dropCommon()
                    if (e.isBoss) { telegraph.visible = false; finish(true) }
                    if (e === rival) finish(true)
                }
            }
            if (player.hp <= 0) finish(false)
            if (mode == Mode.ARENA && state == 0) {
                roundT -= d
                val s = max(0, roundT.toInt()); stageTxt.text = "ARENA   0${s / 60}:${if (s % 60 < 10) "0" else ""}${s % 60}"
                if (roundT <= 0) { val r = rival; finish(r == null || player.hp / player.maxHp >= r.hp / r.maxHp) }
            }

            hpFill.width = 360.0 * (player.hp / player.maxHp).coerceIn(0.0, 1.0)
            coinTxt.text = "COIN ${num(Profile.coins + earned)}"
            if (comboTimer > 0) { comboTimer -= d; if (comboTimer <= 0) combo = 0 }
            comboTxt.text = if (combo >= 2) "$combo COMBO" else ""
            skillLbl.text = if (skillCd > 0) "${skillCd.toInt() + 1}s" else "SKILL"
            potLbl.text = "HP x" + (41..45).sumOf { Profile.inv[it] ?: 0 }
        }
    }
}
