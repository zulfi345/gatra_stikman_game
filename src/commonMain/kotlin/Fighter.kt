package gatra

import korlibs.image.color.*
import korlibs.korge.view.*
import korlibs.math.geom.Angle
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class Look(
    val body: RGBA, val eye: RGBA, val aura: RGBA, val sword: RGBA,
    val band: RGBA? = null, val cape: RGBA? = null, val mask: Boolean = false
)

/** Stickman beranimasi (kaki, tangan, pedang, aura, ikat kepala). Origin = telapak kaki. */
class Fighter(
    val look: Look, val sc: Double, val maxHp: Double, val dmg: Double, val moveSpd: Double,
    val range: Double, val atkCd: Double, val coins: Int = 0,
    val isBoss: Boolean = false, val isPlayer: Boolean = false, showBar: Boolean = true
) : Container() {
    var hp = maxHp
    var facing = 1
    var atkTimer = 0.4 + Random.nextDouble() * 0.6
    var stun = 0.0
    var burn = 0.0
    var burnAcc = 0.0
    val stopDist = range * 0.8 + Random.nextDouble() * 40.0
    private var t = Random.nextDouble() * 6
    private var swingT = 1.0
    private var flash = 0.0
    private val aura: Circle
    private val rig: Container
    private lateinit var legL: Container
    private lateinit var legR: Container
    private lateinit var armB: Container
    private lateinit var armF: Container
    private lateinit var slash: SolidRect
    private var tail: SolidRect? = null
    private val barFill: SolidRect

    init {
        aura = circle(62 * sc, fill = look.aura).xy(-62 * sc, -52 * sc - 62 * sc)
        aura.alpha = 0.16
        rig = container {
            look.cape?.let { solidRect(12 * sc, 50 * sc, it).xy(-17 * sc, -78 * sc).also { r -> r.alpha = 0.9 } }
            armB = container { solidRect(5 * sc, 28 * sc, look.body).xy(-2.5 * sc, 0.0) }.xy(0.0, -72 * sc)
            legL = container { solidRect(6 * sc, 34 * sc, look.body).xy(-3 * sc, 0.0) }.xy(-3 * sc, -34 * sc)
            legR = container { solidRect(6 * sc, 34 * sc, look.body).xy(-3 * sc, 0.0) }.xy(3 * sc, -34 * sc)
            solidRect(9 * sc, 42 * sc, look.body).xy(-4.5 * sc, -76 * sc)
            circle(12 * sc, fill = look.body).xy(-12 * sc, -100 * sc)
            solidRect(8 * sc, 4 * sc, look.eye).xy(1 * sc, -90 * sc)
            look.band?.let {
                solidRect(25 * sc, 5 * sc, it).xy(-12 * sc, -94 * sc)
                tail = solidRect(24 * sc, 4 * sc, it).xy(-30 * sc, -93 * sc)
            }
            if (look.mask) {
                solidRect(26 * sc, 16 * sc, Colors["#c8873a"]).xy(-13 * sc, -97 * sc)
                solidRect(5 * sc, 5 * sc, Colors["#1a0a05"]).xy(-6 * sc, -92 * sc)
                solidRect(5 * sc, 5 * sc, Colors["#1a0a05"]).xy(3 * sc, -92 * sc)
                solidRect(5 * sc, 16 * sc, Colors["#2fbf71"]).xy(-10 * sc, -116 * sc)
                solidRect(5 * sc, 20 * sc, Colors["#e0402e"]).xy(-2 * sc, -120 * sc)
                solidRect(5 * sc, 16 * sc, Colors["#ffd23a"]).xy(6 * sc, -116 * sc)
            }
            slash = solidRect(86 * sc, 9 * sc, look.sword).xy(8 * sc, -52 * sc)
            slash.alpha = 0.0; slash.rotation = Angle.fromDegrees(18.0)
            armF = container {
                solidRect(5 * sc, 26 * sc, look.body).xy(-2.5 * sc, 0.0)
                solidRect(5 * sc, 46 * sc, look.sword).xy(-2.5 * sc, 24 * sc)
            }.xy(0.0, -72 * sc)
            armF.rotation = Angle.fromDegrees(-75.0)
        }
        solidRect(54 * sc, 6.0, Colors["#401010"]).xy(-27 * sc, -126 * sc - 10).also { it.visible = showBar && !isPlayer && !isBoss }
        barFill = solidRect(54 * sc, 6.0, Colors["#ff3b3b"]).xy(-27 * sc, -126 * sc - 10).also { it.visible = showBar && !isPlayer && !isBoss }
    }

    fun face(dir: Int) { if (dir != 0) { facing = dir; rig.scaleX = dir.toDouble() } }
    fun swing() { swingT = 0.0 }
    fun hurt(amount: Double) { hp -= amount; flash = 0.12; rig.alpha = 0.35 }

    fun tick(d: Double, moving: Boolean) {
        atkTimer -= d; t += d
        if (flash > 0) { flash -= d; if (flash <= 0) rig.alpha = 1.0 }
        if (swingT < 1.0) swingT = min(1.0, swingT + d / 0.22)
        val sw = swingT < 1.0
        armF.rotation = Angle.fromDegrees(if (sw) -150.0 + 135.0 * swingT else -75.0 + sin(t * 3) * 4)
        slash.alpha = if (sw) 0.55 * (1 - swingT) else 0.0
        val w = if (moving) sin(t * 14) * 32 else 0.0
        legL.rotation = Angle.fromDegrees(w); legR.rotation = Angle.fromDegrees(-w)
        armB.rotation = Angle.fromDegrees(-w * 0.8 + 8)
        tail?.rotation = Angle.fromDegrees(sin(t * 9) * 12)
        aura.alpha = 0.14 + 0.05 * sin(t * 3)
        rig.y = if (moving) -abs(sin(t * 14)) * 3 else -sin(t * 3)
        barFill.width = 54 * sc * (hp / maxHp).coerceIn(0.0, 1.0)
    }
}
