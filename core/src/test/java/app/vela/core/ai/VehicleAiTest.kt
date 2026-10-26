package app.vela.core.ai

import app.vela.core.model.Drivetrain
import app.vela.core.model.FuelType
import app.vela.core.model.Transmission
import app.vela.core.model.VehicleKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleAiSchemaTest {
    @Test
    fun `every payload writes all canonical keys`() {
        val objectKeys = VehicleAiPayload.empty.toJsonObject().keys

        assertEquals(VehicleAiField.entries.map { it.key }.toSet(), objectKeys)
        assertEquals(VehicleAiField.entries.size, VehicleAiSchema.keys.size)
    }

    @Test
    fun `an unknown payload is all null`() {
        val parsed = Json.parseToJsonElement(VehicleAiSchema.emptyJson).jsonObject

        VehicleAiSchema.keys.forEach { key ->
            assertTrue("$key should be null", parsed[key] is kotlinx.serialization.json.JsonNull)
        }
    }

    @Test
    fun `the schema leaves derived values out`() {
        listOf(
            "rangeKm", "electricRangeKm", "payloadKg", "acceleration", "co2Gkm",
            "costPerKm", "tripCost", "tollClass", "chargingTimeMinutes", "eta",
            "averageSpeedKph", "averageUrbanSpeedKph", "averageExtraUrbanSpeedKph",
            "averageMotorwaySpeedKph",
        ).forEach { derived -> assertFalse(VehicleAiSchema.keys.contains(derived)) }
    }
}

class VehicleAiParserTest {
    @Test
    fun `a complete answer is read`() {
        val payload = VehicleAiParser.parse(
            """
            {
              "make": "Fiat", "model": "Panda", "variant": "1.3 Multijet", "year": 2021,
              "fuelType": "DIESEL", "kind": "CAR", "emissionStandard": "Euro 6d",
              "engineDisplacementCc": 1248, "powerKw": 70, "transmission": "MANUAL",
              "drivetrain": "FWD", "maxSpeedKph": 168, "lengthM": 3.653, "widthM": 1.643,
              "heightM": 1.551, "weightKg": 1055, "tankL": 38,
              "batteryKwh": null, "consumptionL100km": 4.2,
              "consumptionUrbanL100km": 5.1, "consumptionExtraUrbanL100km": 3.7,
              "consumptionMotorwayL100km": 4.6, "energyKwh100km": null,
              "energyUrbanKwh100km": null, "energyExtraUrbanKwh100km": null,
              "energyMotorwayKwh100km": null, "rangeKm": 900
            }
            """.trimIndent(),
        )!!

        assertEquals("Fiat", payload.make)
        assertEquals("Panda", payload.model)
        assertEquals("1.3 Multijet", payload.variant)
        assertEquals(2021, payload.year)
        assertEquals(FuelType.DIESEL, payload.fuelType)
        assertEquals(VehicleKind.CAR, payload.kind)
        assertEquals(1248, payload.engineDisplacementCc)
        assertEquals(70, payload.powerKw)
        assertEquals(Transmission.MANUAL, payload.transmission)
        assertEquals(Drivetrain.FWD, payload.drivetrain)
        assertEquals(3.653, payload.lengthM!!, 0.0001)
        assertEquals(5.1, payload.consumptionUrbanL100km!!, 0.0001)
        assertEquals(3.7, payload.consumptionExtraUrbanL100km!!, 0.0001)
        assertEquals(4.6, payload.consumptionMotorwayL100km!!, 0.0001)
        assertNull(payload.batteryKwh)
    }

    @Test
    fun `missing keys become null and unknown keys are ignored`() {
        val payload = VehicleAiParser.parse("""{"make":"Fiat","model":"Panda","color":"red"}""")!!

        assertEquals("Fiat", payload.make)
        assertEquals("Panda", payload.model)
        assertNull(payload.year)
        assertNull(payload.fuelType)
        assertNull(payload.tankL)
    }

    @Test
    fun `a fenced answer with quoted numbers is accepted`() {
        val payload = VehicleAiParser.parse(
            """
            ```json
            {"make":"Fiat","model":"Panda","year":"2020","powerKw":"51","consumptionL100km":"4,9"}
            ```
            """.trimIndent(),
        )!!

        assertEquals(2020, payload.year)
        assertEquals(51, payload.powerKw)
        assertEquals(4.9, payload.consumptionL100km!!, 0.0001)
    }

    @Test
    fun `enums accept local aliases and unknown values become null`() {
        val payload = VehicleAiParser.parse(
            """
            {"fuelType":"metano","kind":"furgone","transmission":"DSG","drivetrain":"4x4"}
            """.trimIndent(),
        )!!

        assertEquals(FuelType.CNG, payload.fuelType)
        assertEquals(VehicleKind.VAN, payload.kind)
        assertEquals(Transmission.DCT, payload.transmission)
        assertEquals(Drivetrain.AWD, payload.drivetrain)
        assertNull(VehicleAiParser.parse("""{"fuelType":"steam"}""")!!.fuelType)
    }

    @Test
    fun `an implausible number becomes null`() {
        val payload = VehicleAiParser.parse(
            """{"year":1200,"powerKw":-4,"lengthM":900,"widthM":0,"batteryKwh":0}""",
        )!!

        assertNull(payload.year)
        assertNull(payload.powerKw)
        assertNull(payload.lengthM)
        assertNull(payload.widthM)
        assertNull(payload.batteryKwh)
    }

    @Test
    fun `a nested object is unwrapped`() {
        val payload = VehicleAiParser.parse("""{"vehicle":{"make":"Fiat","model":"Panda"}}""")!!

        assertEquals("Fiat", payload.make)
    }

    @Test
    fun `a non JSON answer is rejected`() {
        assertNull(VehicleAiParser.parse("I cannot answer that."))
    }
}

class VehicleAiMergeTest {
    private val criteria = VehicleAiCriteria("Fiat", "Panda", 2020, FuelType.PETROL)

    @Test
    fun `typed identity always wins`() {
        val answer = VehicleAiMerge.merge(
            criteria,
            VehicleAiPayload(
                make = "Volkswagen",
                model = "Golf",
                year = 2019,
                fuelType = FuelType.DIESEL,
                variant = "1.5 TSI",
            ),
        )

        assertEquals("Fiat", answer.payload.make)
        assertEquals("Panda", answer.payload.model)
        assertEquals(2020, answer.payload.year)
        assertEquals(FuelType.PETROL, answer.payload.fuelType)
        assertEquals(
            setOf(
                VehicleAiIdentityField.MAKE,
                VehicleAiIdentityField.MODEL,
                VehicleAiIdentityField.YEAR,
                VehicleAiIdentityField.FUEL_TYPE,
            ),
            answer.conflicts,
        )
        assertEquals("1.5 TSI", answer.payload.variant)
    }

    @Test
    fun `matching identity is not a conflict`() {
        val answer = VehicleAiMerge.merge(
            criteria,
            VehicleAiPayload(make = "fiat", model = "PANDA", year = 2020, fuelType = FuelType.PETROL),
        )

        assertTrue(answer.conflicts.isEmpty())
        assertEquals("Fiat", answer.payload.make)
    }

    @Test
    fun `badges exclude fields the user typed`() {
        val answer = VehicleAiMerge.merge(
            criteria,
            VehicleAiPayload(kind = VehicleKind.CAR, powerKw = 51, variant = "Easy"),
        )

        assertEquals(
            setOf(VehicleAiField.KIND, VehicleAiField.POWER_KW, VehicleAiField.VARIANT),
            answer.badgeFields,
        )
    }

    @Test
    fun `a typed variant wins and carries no badge`() {
        val answer = VehicleAiMerge.merge(
            criteria.copy(variant = "1.3 Multijet 95 CV"),
            VehicleAiPayload(kind = VehicleKind.CAR, variant = "Easy"),
        )

        assertEquals("1.3 Multijet 95 CV", answer.payload.variant)
        assertTrue(answer.typedVariant)
        assertFalse(answer.badgeFields.contains(VehicleAiField.VARIANT))
    }
}

class PlateRedactorTest {
    @Test
    fun `current and old Italian plates are removed`() {
        val result = PlateRedactor.redact("La mia AB123CD e la vecchia AB 123456, non AB123CD.")

        assertFalse(result.text.contains("AB123CD"))
        assertFalse(result.text.contains("AB 123456"))
        assertTrue(result.text.contains(PlateRedactor.PLACEHOLDER))
        assertEquals(3, result.removed)
    }

    @Test
    fun `ordinary vehicle words survive`() {
        val query = "Panda 1.3 Multijet 95 CV Euro 6d del 2021"

        assertEquals(query, PlateRedactor.redact(query).text)
        assertEquals(0, PlateRedactor.redact(query).removed)
    }
}

class VehicleAiPromptTest {
    private val criteria = VehicleAiCriteria("Fiat", "Panda", 2020, FuelType.PETROL)

    @Test
    fun `the variant is included in the user prompt`() {
        val redaction = PlateRedactor.redact("Panda targata AB123CD, 1.3 Multijet")
        val prompt = VehicleAiPrompt.user(criteria.copy(variant = redaction.text))

        assertFalse(prompt.contains("AB123CD"))
        assertTrue(prompt.contains("1.3 Multijet"))
        assertTrue(prompt.contains("Fiat"))
        assertTrue(prompt.contains("Panda"))
        assertTrue(prompt.contains("2020"))
        assertTrue(prompt.contains("PETROL"))
    }

    @Test
    fun `the system prompt asks for every key and excludes derived data`() {
        val prompt = VehicleAiPrompt.system

        VehicleAiSchema.keys.forEach { key -> assertTrue("missing $key", prompt.contains(key)) }
        assertTrue(prompt.contains("Do not return derived values"))
        assertTrue(prompt.contains("null"))
    }
}

class AiProviderRequestsTest {
    private val system = "system prompt"
    private val user = "user prompt"

    @Test
    fun `each provider gets its native request`() {
        val openAi = AiProviderRequests.generate(AiProvider.OPENAI, "gpt-test", "sk-openai", system, user)
        assertEquals("https", openAi.url.scheme)
        assertEquals("api.openai.com", openAi.url.host)
        assertEquals("/v1/responses", openAi.url.encodedPath)
        assertEquals("Bearer sk-openai", openAi.headers["Authorization"])
        assertTrue(openAi.body!!.contains("\"input\""))
        assertTrue(openAi.body.contains("gpt-test"))
        assertTrue(openAi.body.contains("system prompt"))

        val anthropic = AiProviderRequests.generate(AiProvider.ANTHROPIC, "claude-test", "sk-ant", system, user)
        assertEquals("api.anthropic.com", anthropic.url.host)
        assertEquals("/v1/messages", anthropic.url.encodedPath)
        assertEquals("sk-ant", anthropic.headers["x-api-key"])
        assertEquals("2023-06-01", anthropic.headers["anthropic-version"])
        assertTrue(anthropic.body!!.contains("\"system\""))

        val google = AiProviderRequests.generate(AiProvider.GOOGLE, "gemini-test", "goog-key", system, user)
        assertEquals("generativelanguage.googleapis.com", google.url.host)
        assertEquals("goog-key", google.url.queryParameter("key"))
        assertTrue(google.url.encodedPath.endsWith("gemini-test:generateContent"))
        assertTrue(google.body!!.contains("\"responseMimeType\""))

        val router = AiProviderRequests.generate(AiProvider.OPENROUTER, "openai/gpt-test", "sk-or", system, user)
        assertEquals("openrouter.ai", router.url.host)
        assertEquals("/api/v1/chat/completions", router.url.encodedPath)
        assertEquals("Bearer sk-or", router.headers["Authorization"])
        assertTrue(router.body!!.contains("\"response_format\""))
    }

    @Test
    fun `the request body has no plate field`() {
        AiProvider.values().forEach { provider ->
            val body = AiProviderRequests.generate(provider, "model", "key", system, user).body!!
            assertFalse(body.contains("plate", ignoreCase = true))
        }
    }

    @Test
    fun `verification uses an authenticated endpoint`() {
        assertEquals("/v1/models", AiProviderRequests.verify(AiProvider.OPENAI, "k").url.encodedPath)
        assertEquals("/v1/models", AiProviderRequests.verify(AiProvider.ANTHROPIC, "k").url.encodedPath)
        assertEquals("/v1beta/models", AiProviderRequests.verify(AiProvider.GOOGLE, "k").url.encodedPath)
        assertEquals("/api/v1/key", AiProviderRequests.verify(AiProvider.OPENROUTER, "k").url.encodedPath)
    }

    @Test
    fun `each native response yields its text`() {
        assertEquals(
            "{\"make\":\"Fiat\"}",
            AiProviderRequests.extractText(
                AiProvider.OPENAI,
                """{"output_text":"{\"make\":\"Fiat\"}"}""",
            ),
        )
        assertEquals(
            "answer",
            AiProviderRequests.extractText(
                AiProvider.OPENAI,
                """{"output":[{"type":"message","content":[{"type":"output_text","text":"answer"}]}]}""",
            ),
        )
        assertEquals(
            "answer",
            AiProviderRequests.extractText(
                AiProvider.ANTHROPIC,
                """{"content":[{"type":"text","text":"answer"}]}""",
            ),
        )
        assertEquals(
            "answer",
            AiProviderRequests.extractText(
                AiProvider.GOOGLE,
                """{"candidates":[{"content":{"parts":[{"text":"answer"}]}}]}""",
            ),
        )
        assertEquals(
            "answer",
            AiProviderRequests.extractText(
                AiProvider.OPENROUTER,
                """{"choices":[{"message":{"content":"answer"}}]}""",
            ),
        )
    }

    @Test
    fun `a provider error sentence is extracted`() {
        assertEquals(
            "Bad key",
            AiProviderRequests.errorMessage("""{"error":{"message":"Bad key","type":"auth"}}"""),
        )
        assertNull(AiProviderRequests.errorMessage("not json"))
        assertNotNull(AiProviderRequests.errorMessage("""{"type":"error","error":{"message":"Bad key"}}"""))
    }
}
