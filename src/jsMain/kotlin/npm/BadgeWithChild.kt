package npm

import mui.base.BadgeUnstyledProps
import mui.material.Badge
import mui.material.BadgeColor.Companion.primary
import react.FC
import react.ReactNode

external interface BadgeWithChildProps : BadgeUnstyledProps {
    var count: Int
    var childNode: ReactNode
}

val BadgeWithChild = FC<BadgeWithChildProps> { handler ->
    Badge {
        badgeContent = handler.count.unsafeCast<ReactNode>()
        color = primary
        showZero = false
        child(handler.childNode)
    }
}
/*
fun main() {
    document.getElementById("root")?.let { container ->
        val ui = BadgeWithChild.create {
            count = 42
            textContent = "Changes"
        }
        createRoot(container.unsafeCast<Element>()).render(ui)
    }
}*/
