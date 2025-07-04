package com.fusi24.pangreksa.web.base.ui.view

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.applayout.AppLayout
import com.vaadin.flow.component.avatar.Avatar
import com.vaadin.flow.component.avatar.AvatarVariant
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.menubar.MenuBar
import com.vaadin.flow.component.menubar.MenuBarVariant
import com.vaadin.flow.component.orderedlayout.Scroller
import com.vaadin.flow.component.sidenav.SideNav
import com.vaadin.flow.component.sidenav.SideNavItem
import com.vaadin.flow.router.Layout
import com.vaadin.flow.server.menu.MenuConfiguration
import com.vaadin.flow.server.menu.MenuEntry
import com.vaadin.flow.spring.security.AuthenticationContext
import com.fusi24.pangreksa.web.security.CurrentUser
import jakarta.annotation.security.PermitAll
import com.vaadin.flow.theme.lumo.LumoUtility.*

@Layout
@PermitAll // When security is enabled, allow all authenticated users
class MainLayout(
    private val currentUser: CurrentUser,
    private val authenticationContext: AuthenticationContext
) : AppLayout() {

    init {
        primarySection = Section.DRAWER
        addToDrawer(createHeader(), Scroller(createSideNav()), createUserMenu())
    }

    private fun createHeader(): Div {
        // TODO Replace with real application logo and name
        val appLogo = VaadinIcon.CUBES.create().apply {
            addClassNames(TextColor.PRIMARY, IconSize.LARGE)
        }

        val appName = Span("Web").apply {
            addClassNames(FontWeight.SEMIBOLD, FontSize.LARGE)
        }

        return Div(appLogo, appName).apply {
            addClassNames(Display.FLEX, Padding.MEDIUM, Gap.MEDIUM, AlignItems.CENTER)
        }
    }

    private fun createSideNav(): SideNav {
        return SideNav().apply {
            addClassNames(Margin.Horizontal.MEDIUM)
            MenuConfiguration.getMenuEntries().forEach { entry ->
                addItem(createSideNavItem(entry))
            }
        }
    }

    private fun createSideNavItem(menuEntry: MenuEntry): SideNavItem {
        return if (menuEntry.icon() != null) {
            SideNavItem(menuEntry.title(), menuEntry.path(), Icon(menuEntry.icon()))
        } else {
            SideNavItem(menuEntry.title(), menuEntry.path())
        }
    }

    private fun createUserMenu(): Component {
        val user = currentUser.require()

        val avatar = Avatar(user.fullName, user.pictureUrl).apply {
            addThemeVariants(AvatarVariant.LUMO_XSMALL)
            addClassNames(Margin.Right.SMALL)
            colorIndex = 5
        }

        val userMenu = MenuBar().apply {
            addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE)
            addClassNames(Margin.MEDIUM)
        }

        val userMenuItem = userMenu.addItem(avatar).apply {
            add(user.fullName)

            user.profileUrl?.let { profileUrl ->
                subMenu.addItem("View Profile") {
                    UI.getCurrent().page.open(profileUrl)
                }
            }

            // TODO Add additional items to the user menu if needed
            subMenu.addItem("Logout") {
                authenticationContext.logout()
            }
        }

        return userMenu
    }
}
