package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.froniusvisualizer.components.MaterialIcon;
import com.vesanieminen.froniusvisualizer.components.appnav.AppNav;
import com.vesanieminen.froniusvisualizer.components.appnav.AppNavItem;
import com.vesanieminen.froniusvisualizer.util.css.FontFamily;

public class MainLayout extends AppLayout {

    public static final String URL_SUFFIX = " ⋅ Liukuri";

    private H1 title;

    public MainLayout() {
        setPrimarySection(Section.DRAWER);

        // App header
        DrawerToggle toggle = new DrawerToggle();
        toggle.getElement().setAttribute("aria-label", "Menu toggle");

        title = new H1();
        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        Header header = new Header(toggle, title);
        header.addClassNames(LumoUtility.AlignItems.CENTER, LumoUtility.Display.FLEX, LumoUtility.Gap.SMALL,
                LumoUtility.Padding.SMALL);

        addToNavbar(true, header);

        // Navigation sidebar
        Span icon = MaterialIcon.OFFLINE_BOLT.create(LumoUtility.IconSize.LARGE, LumoUtility.TextColor.PRIMARY);

        Span name = new Span("LIUKURI");
        name.addClassNames(FontFamily.LOGO, LumoUtility.FontSize.XXLARGE, LumoUtility.TextColor.PRIMARY);

        Div app = new Div(icon, name);
        app.addClassNames(LumoUtility.AlignItems.CENTER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.XSMALL, LumoUtility.Padding.MEDIUM);

        AppNav nav = new AppNav();
        nav.addClassNames(LumoUtility.Padding.Horizontal.SMALL);
        nav.addItem(new AppNavItem("Graph", NordpoolspotView.class, MaterialIcon.TIMELINE));
        nav.addItem(new AppNavItem("List", PriceListView.class, MaterialIcon.LIST));
        nav.addItem(new AppNavItem("Calculator", PriceCalculatorView.class, MaterialIcon.CALCULATE));

        addToDrawer(app, nav);
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        title.setText(getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
        return title == null ? "" : title.value().replace(URL_SUFFIX, "");
    }
}
