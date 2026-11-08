package ca.gbc.comp3074.nicholas.plantpal.util

import ca.gbc.comp3074.nicholas.plantpal.R

/**
 * Built-in plant illustrations. A plant's photoPath can be "res:<name>" to use one of these
 * instead of a photo file; plants with no photo show the generic potted plant.
 */
object SamplePhotos {
    const val PREFIX = "res:"

    private val drawables = mapOf(
        "plant_monstera" to R.drawable.plant_monstera,
        "plant_basil" to R.drawable.plant_basil,
        "plant_tomatoes" to R.drawable.plant_tomatoes,
        "plant_snake" to R.drawable.plant_snake,
        "plant_lavender" to R.drawable.plant_lavender,
        "plant_generic" to R.drawable.plant_generic
    )

    /** Picture used by each sample plant, keyed by plant name. */
    val forSampleName = mapOf(
        "Monstera" to PREFIX + "plant_monstera",
        "Basil" to PREFIX + "plant_basil",
        "Balcony Tomatoes" to PREFIX + "plant_tomatoes",
        "Snake Plant" to PREFIX + "plant_snake",
        "Lavender" to PREFIX + "plant_lavender"
    )

    fun isBuiltIn(path: String?): Boolean = path?.startsWith(PREFIX) == true

    fun drawableFor(path: String?): Int =
        path?.removePrefix(PREFIX)?.let { drawables[it] } ?: R.drawable.plant_generic
}
