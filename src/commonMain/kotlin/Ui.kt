package gatra

import korlibs.image.color.*
import korlibs.korge.input.*
import korlibs.korge.view.*

fun Container.txt(s: String, x: Double, y: Double, size: Double = 24.0, c: RGBA = Colors.WHITE, w: Double = 0.0): Text {
    val t = text(s, size, c)
    t.xy(if (w > 0) x + (w - s.length * size * 0.52) / 2 else x, y)
    t.mouseEnabled = false
    return t
}

fun Container.panel(x: Double, y: Double, w: Double, h: Double, c: RGBA = Colors["#1b2142"]): SolidRect {
    solidRect(w + 4, h + 4, Colors["#3a4a8a"]).xy(x - 2, y - 2)
    return solidRect(w, h, c).xy(x, y)
}

fun Container.btn(label: String, x: Double, y: Double, w: Double, h: Double, color: RGBA, size: Double = 26.0, onTap: suspend () -> Unit): SolidRect {
    val r = solidRect(w, h, color).xy(x, y)
    r.onClick { onTap() }
    txt(label, x, y + (h - size) / 2 - 3, size, Colors.WHITE, w)
    return r
}

fun Container.popup(msg: String) {
    val layer = container()
    layer.solidRect(1280.0, 720.0, Colors["#000000"]).also { it.alpha = 0.6 }.onClick { }
    layer.panel(320.0, 230.0, 640.0, 260.0, Colors["#1e2340"])
    layer.txt(msg, 340.0, 290.0, 28.0, Colors.WHITE, 600.0)
    layer.btn("OK", 540.0, 400.0, 200.0, 60.0, Colors["#e07b1f"]) { layer.removeFromParent() }
}

fun num(n: Int): String = n.toString().reversed().chunked(3).joinToString(",").reversed()
