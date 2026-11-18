import gatra.LobbyScene
import gatra.Profile
import korlibs.image.color.Colors
import korlibs.korge.Korge
import korlibs.korge.scene.sceneContainer
import korlibs.math.geom.Size

suspend fun main() = Korge(
    windowSize = Size(1280, 720),
    virtualSize = Size(1280, 720),
    backgroundColor = Colors["#12152a"],
) {
    Profile.load(views)
    sceneContainer().changeTo({ LobbyScene() })
}
