import android.content.Context
import com.veganbeauty.app.data.local.RootieDatabase
import kotlinx.coroutines.runBlocking

fun deleteTestPosts(context: Context) {
    val db = RootieDatabase.getDatabase(context)
    val dao = db.communityDao()
    runBlocking {
        // Find all posts where authorId == test_001 OR authorDisplayName == "Test User"
        // Wait, the user uploaded 6 posts. We can just delete them.
        // Actually, we can't run an Android context script directly from command line!
    }
}
