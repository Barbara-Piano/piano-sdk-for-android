package io.piano.android.id

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonAdapter.Factory
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.internal.Util
import io.piano.android.id.PianoIdClient.Companion.parseJwt
import io.piano.android.id.models.PianoIdToken
import io.piano.android.id.models.TokenData

class PianoIdTokeJsonAdapter(
    moshi: Moshi
) : JsonAdapter<PianoIdToken>() {
    private val options: JsonReader.Options = JsonReader.Options.of(
        ACCESS_TOKEN,
        ACCESS_TOKEN_CAMEL,
        REFRESH_TOKEN,
        REFRESH_TOKEN_CAMEL,
        EXPIRES_IN,
        EXPIRES_IN_CAMEL
    )

    private val stringAdapter: JsonAdapter<String> = moshi.adapter(String::class.java)

    private val longAdapter: JsonAdapter<Long> by lazy { moshi.adapter(Long::class.java) }

    private val jwtAdapter: JsonAdapter<TokenData> by lazy { moshi.adapter(TokenData::class.java) }

    override fun fromJson(reader: JsonReader): PianoIdToken {
        var accessToken: String? = null
        var refreshToken: String? = null
        var expiresInTimestamp: Long? = null
        return with(reader) {
            beginObject()
            while (hasNext()) {
                when (selectName(options)) {
                    0, 1 ->
                        accessToken = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                            ACCESS_TOKEN_CAMEL,
                            ACCESS_TOKEN,
                            reader
                        )
                    2, 3 ->
                        refreshToken = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                            REFRESH_TOKEN_CAMEL,
                            REFRESH_TOKEN,
                            reader
                        )
                    4, 5 ->
                        expiresInTimestamp = longAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                            EXPIRES_IN_CAMEL,
                            EXPIRES_IN,
                            reader
                        )
                    -1 -> {
                        // Unknown name, skip it.
                        reader.skipName()
                        reader.skipValue()
                    }
                }
            }
            endObject()
            PianoIdToken(
                accessToken ?: throw Util.missingProperty(ACCESS_TOKEN_CAMEL, ACCESS_TOKEN, reader),
                refreshToken ?: throw Util.missingProperty(REFRESH_TOKEN_CAMEL, REFRESH_TOKEN, reader),
                expiresInTimestamp ?: accessToken!!.parseJwt(jwtAdapter)?.exp ?: 0,
            )
        }
    }

    override fun toJson(writer: JsonWriter, value: PianoIdToken?) {
        if (value == null) {
            throw NullPointerException("value was null! Wrap in .nullSafe() to write nullable values.")
        }
        writer.apply {
            beginObject()
                .name(ACCESS_TOKEN_CAMEL)
            stringAdapter.toJson(this, value.accessToken)
            name(REFRESH_TOKEN_CAMEL)
            stringAdapter.toJson(this, value.refreshToken)
            name(EXPIRES_IN_CAMEL)
            longAdapter.toJson(this, value.expiresInTimestamp)
            endObject()
        }
    }

    companion object {
        private const val ACCESS_TOKEN = "access_token"
        private const val ACCESS_TOKEN_CAMEL = "accessToken"
        private const val REFRESH_TOKEN = "refresh_token"
        private const val REFRESH_TOKEN_CAMEL = "refreshToken"
        private const val EXPIRES_IN = "expires_in"
        private const val EXPIRES_IN_CAMEL = "expiresIn"

        val FACTORY = Factory { type, _, moshi ->
            takeIf { type == PianoIdToken::class.java }?.let { PianoIdTokeJsonAdapter(moshi) }
        }
    }
}
