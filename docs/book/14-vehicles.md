# 14. Vehicles

## What you see

Settings > Vehicles is the local garage. It lists the vehicles saved on this phone, one row
each, with the active one marked by a radio button and an "Active" label. The row's menu has
Edit and Delete. Delete asks first.

Add offers two choices, and only two:

- **Cerca con IA** opens a search form: make, model, year, fuel, an optional plate and an
  optional free-text AI query. AI proposes the technical data, a preview shows what came back,
  then the normal editor opens with an AI badge on every field AI filled.
- **Inserisci manualmente** opens the editor directly. Nothing is sent anywhere.

Existing vehicles always open the editor directly. They never show the AI query, and they carry
no AI badges.

The editor keeps every field editable:

- Identity: make, model, variant, year and an optional plate.
- Vehicle type: car, van, camper, truck or other.
- Fuel: petrol, diesel, LPG, CNG, hybrid, plug-in hybrid, electric, hydrogen or other.
- Routing: Automatic or Truck-aware. Truck-aware stores the intent but does not change routes
  yet.
- Engine and gearbox: emission standard, engine size in cc, power in kW, top speed, transmission
  and drivetrain.
- Dimensions and weight: height, width, width with mirrors, length, weight, gross weight,
  maximum axle load and axle count.
- Consumption and energy: fuel consumption in L/100 km, electric consumption in kWh/100 km,
  consumption cycle, battery size, tank size, maximum charging power and charging connector.
- Notes.

Make, model, year, fuel and vehicle type are required. Numbers may use a comma or a dot. A
blank number is fine; a non-number is highlighted and blocks Save. The plate and a note that the
garage is not backed up are shown on both the list and the editor.

AI badges are per field and live only in the editor. Changing that field removes its badge. The
badge and the AI query are never written to `VehicleProfile` or its JSON.

In the route picker, drive mode shows a chip with the active vehicle's name. Tapping it lists
the garage and switches the active vehicle. The chip is absent while the garage is empty. It
does not change the route, the tolls or the fuel cost.

## AI settings

Settings > AI chooses exactly one active provider and stores its API key and model id. Tapping a
row only selects it for editing; **Save and use** makes it the active provider:

| Provider | Native request | Default model |
|---|---|---|
| OpenAI | `POST https://api.openai.com/v1/responses` | `gpt-4.1-mini` |
| Anthropic | `POST https://api.anthropic.com/v1/messages` | `claude-sonnet-4-5` |
| Google | `POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent` | `gemini-2.5-flash` |
| OpenRouter | `POST https://openrouter.ai/api/v1/chat/completions` | `openai/gpt-4.1-mini` |

The model id is a text field because provider catalogs change. "Test key" asks OpenAI,
Anthropic and Google for their model list, and OpenRouter for the current key (its model list is
public and cannot prove a key). It does not spend generation tokens. There is no built-in key, Vela does not pay for a request, and a failure on the active
provider is reported; Vela never retries on another provider behind your back.

The search form only needs make, model, year and fuel. Kind is proposed by AI and stays
required in the editor. The plate is collected in the form but is never put in the request: the
request type itself has no plate field. The free-text query is optional, goes to the active
provider, and is stripped of plate-like tokens first.

## Where the data comes from

The garage itself is local. It is written to the `vela_vehicles` SharedPreferences file as a
JSON list under `vehicles`, with the active id under `active_vehicle_id`.

The AI search uses the one provider chosen in Settings > AI. The request carries the structured
make, model, year and fuel, the redacted free text, and a shared JSON schema. It never carries
the plate, notes, routing mode or the saved garage.

API keys live in the separate `vela_ai` SharedPreferences file as AES-GCM ciphertext. The
AES-256 key is generated in the Android Keystore under alias `vela_ai_key` and is not exportable.
`vela_ai` is explicitly excluded from Android backup and phone-to-phone transfer in
`backup_rules.xml` and `data_extraction_rules.xml`, so a restored phone has no key. Decryption
failure reads as "no key" and the key must be entered again.

The vehicle file is left out of the same backup lists too: a plate and vehicle details stay on
the phone. There is no export or import yet.

## How it is decided

`VehicleStore` is a singleton in `core/data/VehicleStore.kt`. It publishes two `StateFlow`s
through `VehiclePrefs`: the whole garage and the active profile. It publishes once when it is
constructed and after every write, so the Settings page and the route chip see a change without
another callback through `MapScreen`. Save, delete and activate are called by
`VehicleSettingsViewModel`; `MapViewModel` still exposes its garage methods unchanged for the
route chip.

The JSON sets `ignoreUnknownKeys`, so a payload with fields from a newer build still decodes.
Invalid JSON reads as an empty garage. On save, make, model, variant, plate, emission standard,
charging connector and notes are trimmed, an update keeps its original creation time, and the
entry moves to the front of the list.

The first saved vehicle becomes active. Deleting the active vehicle promotes the first
remaining one. An unknown or missing active id falls back to the first vehicle, and an empty
garage has no active vehicle. The route chip writes through `VehicleActions.setActive`, which
`MapViewModel` points at the store. The store does not touch routes, tolls, fuel prices or
search.

### The shared AI shape

`VehicleAiSchema` is the contract for all four providers. Every answer is normalized to the same
27 keys, and an unknown value is `null`:

`make`, `model`, `variant`, `year`, `fuelType`, `kind`, `emissionStandard`,
`engineDisplacementCc`, `powerKw`, `transmission`, `drivetrain`, `maxSpeedKph`, `lengthM`,
`widthM`, `widthWithMirrorsM`, `heightM`, `weightKg`, `grossWeightKg`, `axleLoadKg`,
`axleCount`, `tankL`, `batteryKwh`, `consumptionL100km`, `energyKwh100km`,
`consumptionStandard`, `maxChargingPowerKw`, `chargingConnector`.

Units are cc, kW, km/h, meters, kilograms, liters and kWh per 100 km. `batteryKwh` is usable
capacity when known. `axleLoadKg` is the maximum axle load. The parser accepts quoted numbers
and common English/Italian enum aliases (`PHEV`, `metano`, `DSG`, `4x4`), ignores unknown keys,
and turns an unknown enum, a blank string or an implausible number into `null`.

Derived values are deliberately absent from the schema: range and electric range, payload,
power-to-weight, acceleration, CO2, cost per kilometer, trip cost, toll class, charging time and
ETA. Torque, wheelbase, seats, doors, towing capacity and 0-100 time are also left for later
sources.

### Identity, conflicts and privacy

`VehicleAiCriteria` contains make, model, year and fuel. It has no plate field, so the plate
cannot reach a provider through the request type. Before the prompt is built, `PlateRedactor`
replaces current Italian/EU plates (two letters, three digits, two letters), old Italian plates
(two letters, six digits), and one-letter three-digit three-letter formats, with optional space,
dot or dash. The preview and editor say how many tokens were removed.

`VehicleAiMerge` restores the typed make, model, year and fuel after the provider answers. If the
provider returned a different non-null value, the field is listed in a conflict warning but the
typed value is kept. The other fields fill the editor. A failed request shows the provider's
error, a connection error or a format error; there is no fallback to another provider.

The prompt, schema and parser are shared in `core/ai/`; the four adapters only build the HTTP
envelope and read the native response path. The AI HTTP client has connect 20 s, read 90 s,
write 30 s and call 100 s, and is built from the shared client so it does not inherit the 12 s
map timeout. The free-text AI query and the AI badges live only in creation/editor UI state.

## Limits

- The AI data is only as good as the model's answer. Every field stays editable, and the editor
  does not check that a height, weight or axle load is plausible for the vehicle type.
- The model id is a free-text field; a provider can retire one and the error appears as a failed
  request.
- No FleetByte or EEA CO2 catalog, no multi-database merge, no plate lookup, and no ChatGPT
  account/OAuth. Only user-supplied API keys are supported.
- No export or import, and no Android backup. Uninstalling the app or clearing its data removes
  the garage and the encrypted keys.
- Truck-aware routing, offline truck limits, toll estimates and fuel costs are not connected
  yet. The routing mode and the measurements are stored for those later phases.
- The garage is per phone. It does not sync to Android Auto, another device or a Vela account.
