package com.uc.caffeine

import android.app.Application
import android.content.Context
import com.uc.caffeine.monetization.DistributionServices
import com.uc.caffeine.monetization.FlavorServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

class CaffeineApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(com.uc.caffeine.util.AppLocale.wrap(base))
    }
    val distributionServices: DistributionServices by lazy { FlavorServices.create(this) }

    override fun onCreate() {
        super.onCreate()
        // Helper processes (e.g. Play's icon-switch restarter) only run their own component.
        if (getProcessName() != packageName) return
        com.uc.caffeine.util.AppLocale.migrateToPlatform(this)
        distributionServices
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            distributionServices.featureAccess.snapshots
                .distinctUntilChangedBy { listOf(it.source, it.expired, it.verification) }
                .drop(1)
                .collect { com.uc.caffeine.widget.CaffeineWidgetUpdater.update(this@CaffeineApplication) }
        }
    }
}

val Context.distributionServices: DistributionServices
    get() = (applicationContext as CaffeineApplication).distributionServices
