# 14. Vehicles

## What you see

Settings > Vehicles is the local garage. It lists the vehicles saved on this phone, one row
each, with the active one marked by a radio button and an "Active" label. Add opens a manual
editor; the row's menu has Edit and Delete. Delete asks first.

The editor keeps every field editable:

- Identity: nickname, make, model, year and an optional plate.
- Vehicle type: car, van, camper, truck or other.
- Fuel: petrol, diesel, LPG, CNG, hybrid, plug-in hybrid, electric, hydrogen or other.
- Routing: Automatic or Truck-aware. Truck-aware stores the intent but does not change routes
  yet.
- Dimensions and weight: height, width, width with mirrors, length, weight, gross weight,
  maximum axle load and axle count.
- Consumption: fuel consumption in L/100 km, electric consumption in kWh/100 km, battery size
  and tank size.
- Notes.

Make and model are required. Numbers may use a comma or a dot. A blank number is fine; a
non-number is highlighted and blocks Save. The plate and a note that the garage is not backed
up are shown on both the list and the editor.

In the route picker, drive mode shows a chip with the active vehicle's name. Tapping it lists
the garage and switches the active vehicle. The chip is absent while the garage is empty. It
does not change the route, the tolls or the fuel cost.

## Where the data comes from

Nothing here uses the network. The garage is written to the `vela_vehicles` SharedPreferences
file as a JSON list under `vehicles`, with the active id under `active_vehicle_id`. The data is
entered by hand. No catalog, plate lookup service or AI provider is asked.

The file is deliberately left out of the Android backup include lists in `backup_rules.xml` and
`data_extraction_rules.xml`. A plate and vehicle details stay on the phone. There is no export
or import yet.

## How it is decided

`VehicleStore` is a singleton in `core/data/VehicleStore.kt`. It publishes two `StateFlow`s
through `VehiclePrefs`: the whole garage and the active profile. It publishes once when it is
constructed and after every write, so the Settings page and the route chip see a change without
another callback through `MapScreen`.

The JSON sets `ignoreUnknownKeys`, so a payload with fields from a newer build still decodes.
Invalid JSON reads as an empty garage. On save, the nickname, make, model, plate and notes are
trimmed, an update keeps its original creation time, and the entry moves to the front of the
list.

The first saved vehicle becomes active. Deleting the active vehicle promotes the first
remaining one. An unknown or missing active id falls back to the first vehicle, and an empty
garage has no active vehicle. The route chip writes through `VehicleActions.setActive`, which
`MapViewModel` points at the store. The store does not touch routes, tolls, fuel prices or
search.

The chip is in both route pickers: the Google-style picker and the classic directions panel.
It appears only in drive mode.

## Limits

- Manual entry is the only source. There is no make and model catalog, so a typo is saved as
  typed.
- No plate lookup. A future provider would be optional and user-keyed; the plate never leaves
  the phone today.
- No export or import, and no Android backup. Uninstalling the app or clearing its data removes
  the garage.
- Truck-aware routing, offline truck limits, toll estimates and fuel costs are not connected
  yet. The routing mode and the measurements are stored for those later phases.
- The editor checks make and model and that numbers parse. It does not check that a height,
  weight or axle load is plausible for the vehicle type.
- The garage is per phone. It does not sync to Android Auto, another device or a Vela account.
