package com.fusi24.pangreksa.web.base.ui.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Main
import com.vaadin.flow.router.Route
import com.vaadin.flow.theme.lumo.LumoUtility
import com.fusi24.pangreksa.web.base.ui.component.ViewToolbar
import jakarta.annotation.security.PermitAll

/**
 * This view shows up when a user navigates to the root ('/') of the application.
 */
@Route("")
@PermitAll // When security is enabled, allow all authenticated users
class MainView : Main() {

    init {
        addClassName(LumoUtility.Padding.MEDIUM)
        add(ViewToolbar("Main"))
        add(Div("Please select a view from the menu on the left."))
    }

    companion object {
        /**
         * Navigates to the main view.
         */
        fun showMainView() {
            UI.getCurrent().navigate(MainView::class.java)
        }
    }
}
