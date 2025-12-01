package com.example.ut2_app.util

import com.example.ut2_app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp

object SupabaseClientProvider {
    val supabase: SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        // CRÍTICO: Crear instancia de OkHttp para soporte de WebSockets
        httpEngine = OkHttp.create {}

        install(Auth)
        install(Postgrest)
        install(Storage)
        install(Realtime)
    }
}