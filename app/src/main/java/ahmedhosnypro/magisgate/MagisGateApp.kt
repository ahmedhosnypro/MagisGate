package ahmedhosnypro.magisgate

import android.app.Application
import com.topjohnwu.superuser.Shell

class MagisGateApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.enableLegacyStderrRedirection = true
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setTimeout(10)
        )
    }
}
