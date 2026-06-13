import java.io.File

fun main() {
    val content = File("app/src/main/assets/community_posts.json").readText()
    try {
        val json = org.json.JSONObject(content)
        println("Success parsing JSONObject")
    } catch (e: Exception) {
        println("Failed JSONObject: ${e.message}")
    }
}
