package org.setu.placemark

import org.setu.placemark.models.PlacemarkModel


interface OnMarkListener {

    fun onMarkClick(mark: PlacemarkModel)

    fun onMarkLongClick(mark: PlacemarkModel): Boolean

}

