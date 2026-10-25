package app.vela.ui

/**
 * The route picker's vehicle chip switches the active vehicle through here, set by
 * [app.vela.ui.map.MapViewModel]. A holder because the pickers are reached from `MapScreen`,
 * which cannot take another callback or parameter (the verifier and method-size limits).
 */
object VehicleActions {
    @Volatile var setActive: ((id: String) -> Unit)? = null
}
