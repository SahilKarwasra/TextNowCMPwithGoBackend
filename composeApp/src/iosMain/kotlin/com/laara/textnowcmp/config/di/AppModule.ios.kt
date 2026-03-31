package com.laara.textnowcmp.config.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.laara.textnowcmp.config.database.TextNowDatabase
import com.laara.textnowcmp.config.datastore.DataStoreRepository
import com.laara.textnowcmp.core.shared.ContactsReader
import com.laara.textnowcmp.core.webrtc.WebRtcPeerConnectionFactory
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

private const val DATA_STORE_FILE_NAME = "textnow_prefs.preferences_pb"

actual val platformModule = module {
    single<HttpClientEngine> {
        Darwin.create {
            configureRequest {
                setTimeoutInterval(30.0)
                setAllowsCellularAccess(true)
            }
        }
    }

    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.createWithPath(
            produceFile = {
                val documentDirectory = documentDirectory()
                "$documentDirectory/$DATA_STORE_FILE_NAME".toPath()
            }
        )
    }

    single { DataStoreRepository(get()) }
    single { ContactsReader() }

    single<TextNowDatabase> {
        val dbPath = "${documentDirectory()}/textnow.db"
        Room.databaseBuilder<TextNowDatabase>(
            name = dbPath,
        )
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    // WebRTC
    single { WebRtcPeerConnectionFactory() }
}

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentDirectory?.path) {
        "Could not find iOS document directory"
    }
}