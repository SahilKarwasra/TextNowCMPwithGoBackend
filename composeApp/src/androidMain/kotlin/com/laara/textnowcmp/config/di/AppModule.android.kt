package com.laara.textnowcmp.config.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.laara.textnowcmp.config.datastore.DataStoreRepository
import com.laara.textnowcmp.core.shared.ContactsReader
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module

private const val DATA_STORE_FILE_NAME = "textnow_prefs.preferences_pb"

actual val platformModule = module {
    single<HttpClientEngine> {
        OkHttp.create {
            config {
                retryOnConnectionFailure(true)
                connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            }
        }
    }

    single<DataStore<Preferences>> {
        val context: Context = get()
        PreferenceDataStoreFactory.createWithPath(
            produceFile = {
                context.filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath.toPath()
            }
        )
    }

    single { DataStoreRepository(get()) }
    single { ContactsReader(context = get()) }
}