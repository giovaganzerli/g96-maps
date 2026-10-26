# 14. Vehicles

## What you see

Settings > Vehicles is the local garage. It lists the vehicles saved on this phone, one row
each, with the active one marked by a radio button and an "Active" label. The row's menu has
Edit and Delete. Delete asks first.

**Aggiungi veicolo** opens the AI search directly when the active AI provider has a saved key.
If it does not, the manual editor opens instead. When AI is configured, the AI form still has a
secondary **Inserisci manualmente** action, so the manual path is always reachable. Existing
vehicles always open the editor directly; they never show the AI form and start with no AI
badge. A weight recalculation can add consumption badges later.

The editor is split into these sections:

- Identity: plate, make, model, version/trim, year and vehicle type.
- Engine and transmission: engine displacement in cc, power in kW, fuel, emission class,
  gearbox (manual, automatic or not specified) and drivetrain (front, rear, all or not
  specified).
- Dimensions and weights: length, width, height and weight.
- Energy and consumption: tank and/or battery capacity, then overall, urban, extra-urban and
  motorway fuel consumption (L/100 km) and/or electric consumption (kWh/100 km).
- Speed: overall, urban, extra-urban and motorway average speed, then top speed.

Only **Auto**, **Furgone** and **Camper** are offered as vehicle types, and only **Benzina**,
**Diesel**, **GPL**, **Metano**, **Ibrida**, **Ibrida Plug-in** and **Elettrica** as fuels.
Older entries that already contain a legacy type or fuel keep that value visible while they are
edited, so existing data is not silently rewritten. The old **Percorso** section and its
subitems are gone.

Make, model, year, fuel and type are required and marked with an asterisk. Numbers may use a
comma or a dot. A blank number is fine; a non-number or an implausible value is highlighted and
blocks Save. The emission class is picked from a fixed list: Euro 0 through Euro 7, including
5a, 5b, 5+, 6a-6e, 6d-TEMP, 6d-ISC-FCM and 6e-bis, plus "not specified".

Fuel-dependent fields appear only when they apply:

- Electric hides the tank, engine displacement, emission class and fuel-consumption fields.
- Petrol, diesel, LPG, CNG and hydrogen hide the battery and electric-consumption fields.
- Hybrid shows tank, battery and fuel consumption.
- Plug-in hybrid shows tank, battery, fuel consumption and electric consumption.

Changing fuel clears every value and AI badge that no longer applies. If AI returned a diesel
tank and the user switches to electric, the tank is removed before the editor is shown.

The weight field has **Ricalcola i consumi con IA**. It is enabled once make, model, year, fuel,
type and weight are valid. It sends the main vehicle data, the current fuel and electric
figures, the weight those figures refer to, and the new weight to the active provider. The
answer adjusts them for the new weight and fills only the figures that apply to the selected
fuel. Every updated field gets the same transient AI badge as a search result; a null figure
leaves the old value alone. If any input changes while the request is running, the answer is
discarded and the editor asks for another recalculation. A successful recalculation makes the
new weight the reference for the next change. There is no local formula and no second provider.

Older fields kept in `VehicleProfile` for JSON compatibility but no longer shown are routing
mode, width with mirrors, gross weight, axle load, axle count, consumption cycle, maximum
charging power, charging connector and notes. They still decode and are written back from an
old entry, but they are not part of the new editor.

AI badges are per field and live only in the editor. Changing that field removes its badge. A
version/trim typed by the user is not marked as AI even when AI repeats it. The badges, the
search form and the recalculation state are never written to `VehicleProfile`; the chosen
version/trim becomes the normal `variant` field and is saved with the vehicle.

In the route picker, drive mode shows a chip with the active vehicle's name. Tapping it lists
the garage and switches the active vehicle. The chip is absent while the garage is empty. It
does not change the route, the tolls or the fuel cost.

## AI settings

Settings > AI chooses exactly one active provider and stores its API key and model id. The
wording is deliberately generic: the active provider will serve every AI feature, not only the
garage. Tapping a row only selects it for editing; **Salva** makes it the active provider.

| Provider | Native request | Default model |
|---|---|---|
| OpenAI | `POST https://api.openai.com/v1/responses` | `gpt-4.1-mini` |
| Anthropic | `POST https://api.anthropic.com/v1/messages` | `claude-sonnet-4-5` |
| Google | `POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent` | `gemini-2.5-flash` |
| OpenRouter | `POST https://openrouter.ai/api/v1/chat/completions` | `openai/gpt-4.1-mini` |

A saved key is shown as a fixed bullet mask, never as the real key and never as an empty field.
The first edit replaces the mask with the new value. **Salva** and **Elimina** share one row. A
blank key on save keeps the stored one; Elimina deletes it. The small **Prova** group below
tests the saved key.

The model id is a text field because provider catalogs change. "Test key" asks OpenAI,
Anthropic and Google for their model list, and OpenRouter for the current key (its model list is
public and cannot prove a key). It does not spend generation tokens. There is no built-in key,
Vela does not pay for a request, and a failure on the active provider is reported; Vela never
retries on another provider behind your back.

## AI search

The search form asks for exactly these fields:

- Make, required.
- Model, required.
- Version/trim, optional. This is also the place for any detail that helps identify the vehicle.
- Year, required.
- Fuel, required, using the seven fuels above.

There is no plate field in the AI form. The plate remains a private, local field in the manual
editor. The version/trim text goes to the active provider, but `PlateRedactor` first replaces
current Italian/EU plates (two letters, three digits, two letters), old Italian plates (two
letters, six digits), and one-letter three-digit three-letter formats, with optional space, dot
or dash. The form warns how many tokens were removed.

The provider answers one JSON object with exactly these 26 keys, all present and `null` when
unknown:

`make`, `model`, `variant`, `year`, `fuelType`, `kind`, `emissionStandard`,
`engineDisplacementCc`, `powerKw`, `transmission`, `drivetrain`, `maxSpeedKph`, `lengthM`,
`widthM`, `heightM`, `weightKg`, `tankL`, `batteryKwh`, `consumptionL100km`,
`consumptionUrbanL100km`, `consumptionExtraUrbanL100km`, `consumptionMotorwayL100km`,
`energyKwh100km`, `energyUrbanKwh100km`, `energyExtraUrbanKwh100km`,
`energyMotorwayKwh100km`.

Units are cc, kW, km/h, meters, kilograms, liters and per-100-km figures. `batteryKwh` is usable
capacity when known. The parser accepts quoted numbers and common English/Italian enum aliases
(`PHEV`, `metano`, `DSG`, `4x4`), ignores unknown keys, and turns an unknown enum, a blank string
or an implausible number into `null`.

The weight recalculation uses a separate schema with only the eight consumption figures:
`consumptionL100km`, `consumptionUrbanL100km`, `consumptionExtraUrbanL100km`,
`consumptionMotorwayL100km`, `energyKwh100km`, `energyUrbanKwh100km`,
`energyExtraUrbanKwh100km` and `energyMotorwayKwh100km`. Its request type,
`VehicleConsumptionInput`, carries the main vehicle data, the current figures, the reference
weight and the new weight. It has no plate, notes or average-speed fields, and the variant is
redacted before it reaches the provider. The prompt asks for the current figures to be adjusted
to the new weight, keeping the cycle and conditions, and estimates a figure only when the
reference data is missing. The parser accepts the same wrapping and quoted numbers as the search
parser. An answer with no usable figure is a distinct empty-answer error.

Average driver speeds and trip averages are deliberately absent: those are behavior, not
manufacturer data. They are manual-only fields in the editor. Other derived values are also
excluded: range and electric range, payload, power-to-weight, acceleration, CO2, cost per
kilometer, trip cost, toll class, charging time and ETA.

`VehicleAiCriteria` contains make, model, year, fuel and the optional version/trim `variant`. It
has no plate field, so the plate cannot reach a provider through the request type. After the
answer, `VehicleAiMerge` restores the typed make, model, year, fuel and typed variant. If the
provider returned a different identity value, the field is listed in a conflict warning but the
typed value is kept. The result opens a preview; **Apri nell'editor** opens the normal editor
with the AI badges. The weight recalculation fills the editor in place, without a preview. A
failed request shows the provider's error, a connection error or a format error; an empty answer
has its own message. There is no fallback to another provider.

The prompt, schema and parser are shared in `core/ai/`; the four adapters only build the HTTP
envelope and read the native response path. The AI HTTP client has connect 20 s, read 90 s,
write 30 s and call 100 s, and is built from the shared client so it does not inherit the 12 s
map timeout.

## Where the data comes from

The garage itself is local. It is written to the `vela_vehicles` SharedPreferences file as a
JSON list under `vehicles`, with the active id under `active_vehicle_id`.

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

## Limits

- The AI data is only as good as the model's answer. Every field stays editable, and the editor
  does not check that a height, weight or consumption figure is plausible for the vehicle type.
  A recalculated consumption figure is an estimate, not a measured value.
- The model id is a free-text field; a provider can retire one and the error appears as a failed
  request.
- No FleetByte or EEA CO2 catalog, no multi-database merge, no plate lookup, and no ChatGPT
  account/OAuth. Only user-supplied API keys are supported.
- No export or import, and no Android backup. Uninstalling the app or clearing its data removes
  the garage and the encrypted keys.
- Truck-aware routing, offline truck limits, toll estimates and fuel costs are not connected
  yet. The measurements are stored for those later phases.
- The garage is per phone. It does not sync to Android Auto, another device or a Vela account.
