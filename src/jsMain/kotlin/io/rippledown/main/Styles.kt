package io.rippledown.main

import mui.material.GridProps
import web.cssom.px
import web.cssom.rgb

val px0 = 0.px
val px4 = 4.px
val px8 = 8.px
val px12 = 12.px

val rd = rgb(24, 24, 198)
val blue = rgb(24, 24, 198)
val red = rgb(240, 200, 200)
val green = rgb(200, 240, 200)
val white = rgb(255, 255, 255)

inline var GridProps.xs: Any?
    get() = asDynamic().xs
    set(value) {
        asDynamic().xs = value
    }


