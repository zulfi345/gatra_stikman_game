import korlibs.korge.gradle.*

plugins { id("com.soywiz.korge") version "5.4.0" }

korge {
    id = "com.gatra.stickman"
    name = "Gatra Stickman"
    orientation = Orientation.LANDSCAPE
    targetJvm()
    targetAndroid()
}
