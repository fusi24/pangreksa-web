package com.fusi24.pangreksa.web.base.ui.component

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.Composite
import com.vaadin.flow.component.applayout.DrawerToggle
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.Header
import com.vaadin.flow.theme.lumo.LumoUtility
import com.vaadin.flow.theme.lumo.LumoUtility.Flex
import com.vaadin.flow.theme.lumo.LumoUtility.Margin

class ViewToolbar(viewTitle: String?, vararg components: Component?) : Composite<Header?>() {
    init {
        addClassNames(
            LumoUtility.Display.FLEX,
            LumoUtility.FlexDirection.COLUMN,
            LumoUtility.JustifyContent.BETWEEN,
            LumoUtility.AlignItems.STRETCH,
            LumoUtility.Gap.MEDIUM,
            LumoUtility.FlexDirection.Breakpoint.Medium.ROW,
            LumoUtility.AlignItems.Breakpoint.Medium.CENTER
        )

        val drawerToggle = DrawerToggle()
        drawerToggle.addClassNames(Margin.NONE)

        val title = H1(viewTitle)
        title.addClassNames(LumoUtility.FontSize.XLARGE, Margin.NONE, LumoUtility.FontWeight.LIGHT)

        val toggleAndTitle = Div(drawerToggle, title)
        toggleAndTitle.addClassNames(LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER)
        getContent()!!.add(toggleAndTitle)

        if (components.size > 0) {
            val actions = Div(*components)
            actions.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.JustifyContent.BETWEEN,
                Flex.GROW,
                LumoUtility.Gap.SMALL,
                LumoUtility.FlexDirection.Breakpoint.Medium.ROW
            )
            getContent()!!.add(actions)
        }
    }

    companion object {
        fun group(vararg components: Component?): Component {
            val group = Div(*components)
            group.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.AlignItems.STRETCH,
                LumoUtility.Gap.SMALL,
                LumoUtility.FlexDirection.Breakpoint.Medium.ROW,
                LumoUtility.AlignItems.Breakpoint.Medium.CENTER
            )
            return group
        }
    }
}
