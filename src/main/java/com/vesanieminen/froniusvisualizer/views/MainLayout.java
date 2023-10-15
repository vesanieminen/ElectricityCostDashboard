package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.charts.ChartOptions;
import com.vaadin.flow.component.charts.model.Lang;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.froniusvisualizer.components.MaterialIcon;
import com.vesanieminen.froniusvisualizer.util.css.FontFamily;
import jakarta.servlet.http.Cookie;

import java.text.DecimalFormat;

import static com.vesanieminen.froniusvisualizer.util.Utils.enLocale;
import static com.vesanieminen.froniusvisualizer.util.Utils.fiLocale;

public class MainLayout extends AppLayout {

    public static final String URL_SUFFIX = " ⋅ Liukuri";

    public boolean darkMode = false;

    public static final String DISCORD_SVG = "<svg class=\"icon-s\" fill=\"var(--lumo-primary-text-color)\" xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 127.14 96.36\"><g id=\"图层_2\" data-name=\"图层 2\"><g id=\"Discord_Logos\" data-name=\"Discord Logos\"><g id=\"Discord_Logo_-_Large_-_White\" data-name=\"Discord Logo - Large - White\"><path d=\"M107.7,8.07A105.15,105.15,0,0,0,81.47,0a72.06,72.06,0,0,0-3.36,6.83A97.68,97.68,0,0,0,49,6.83,72.37,72.37,0,0,0,45.64,0,105.89,105.89,0,0,0,19.39,8.09C2.79,32.65-1.71,56.6.54,80.21h0A105.73,105.73,0,0,0,32.71,96.36,77.7,77.7,0,0,0,39.6,85.25a68.42,68.42,0,0,1-10.85-5.18c.91-.66,1.8-1.34,2.66-2a75.57,75.57,0,0,0,64.32,0c.87.71,1.76,1.39,2.66,2a68.68,68.68,0,0,1-10.87,5.19,77,77,0,0,0,6.89,11.1A105.25,105.25,0,0,0,126.6,80.22h0C129.24,52.84,122.09,29.11,107.7,8.07ZM42.45,65.69C36.18,65.69,31,60,31,53s5-12.74,11.43-12.74S54,46,53.89,53,48.84,65.69,42.45,65.69Zm42.24,0C78.41,65.69,73.25,60,73.25,53s5-12.74,11.44-12.74S96.23,46,96.12,53,91.08,65.69,84.69,65.69Z\"/></g></g></g></svg>";
    public static final String GITHUB_SVG = "<svg class=\"icon-s\" viewBox=\"0 0 98 96\" xmlns=\"http://www.w3.org/2000/svg\"><path fill-rule=\"evenodd\" clip-rule=\"evenodd\" d=\"M48.854 0C21.839 0 0 22 0 49.217c0 21.756 13.993 40.172 33.405 46.69 2.427.49 3.316-1.059 3.316-2.362 0-1.141-.08-5.052-.08-9.127-13.59 2.934-16.42-5.867-16.42-5.867-2.184-5.704-5.42-7.17-5.42-7.17-4.448-3.015.324-3.015.324-3.015 4.934.326 7.523 5.052 7.523 5.052 4.367 7.496 11.404 5.378 14.235 4.074.404-3.178 1.699-5.378 3.074-6.6-10.839-1.141-22.243-5.378-22.243-24.283 0-5.378 1.94-9.778 5.014-13.2-.485-1.222-2.184-6.275.486-13.038 0 0 4.125-1.304 13.426 5.052a46.97 46.97 0 0 1 12.214-1.63c4.125 0 8.33.571 12.213 1.63 9.302-6.356 13.427-5.052 13.427-5.052 2.67 6.763.97 11.816.485 13.038 3.155 3.422 5.015 7.822 5.015 13.2 0 18.905-11.404 23.06-22.324 24.283 1.78 1.548 3.316 4.481 3.316 9.126 0 6.6-.08 11.897-.08 13.526 0 1.304.89 2.853 3.316 2.364 19.412-6.52 33.405-24.935 33.405-46.691C97.707 22 75.788 0 48.854 0z\" fill=\"var(--lumo-primary-text-color)\"/></svg>";

    private Header header;
    private H1 title;

    public MainLayout() {
        setPrimarySection(Section.DRAWER);

        // App header
        DrawerToggle toggle = new DrawerToggle();
        toggle.getElement().setAttribute("aria-label", "Menu toggle");

        title = new H1();
        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.End.AUTO, LumoUtility.Margin.Vertical.NONE);

        header = new Header(toggle, title);
        header.addClassNames(LumoUtility.AlignItems.CENTER, LumoUtility.Display.FLEX, LumoUtility.Gap.SMALL,
                LumoUtility.Padding.SMALL);
        header.setWidthFull();

        addToNavbar(true, header);

        // Navigation sidebar
        final var icon = new Image("icons/icon.png", "Liukuri");
        icon.setMaxWidth(140, Unit.PIXELS);

        Span name = new Span("LIUKURI");
        name.addClassNames(FontFamily.LOGO, LumoUtility.FontSize.XXLARGE, LumoUtility.TextColor.PRIMARY);

        Div app = new Div(icon, name);
        app.addClassNames(LumoUtility.AlignItems.CENTER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.XSMALL, LumoUtility.Padding.MEDIUM);

        final var nav = new SideNav();
        nav.addClassNames(LumoUtility.Padding.Horizontal.SMALL);
        nav.addItem(new SideNavItem(getTranslation("Chart"), NordpoolspotView.class, MaterialIcon.TIMELINE.create()));
        nav.addItem(new SideNavItem(getTranslation("column.chart"), ChartTemplateView.class, MaterialIcon.BAR_CHART.create()));
        nav.addItem(new SideNavItem(getTranslation("Market electricity group"), ChartTemplateViewForTimo.class, MaterialIcon.BAR_CHART.create()));
        nav.addItem(new SideNavItem(getTranslation("view.history"), HistoryView.class, MaterialIcon.HISTORY.create()));
        nav.addItem(new SideNavItem(getTranslation("List"), PriceListView.class, MaterialIcon.LIST.create()));
        //nav.addItem(new SideNavItem(getTranslation("Monthly Prices"), MonthlyPricesView.class, MaterialIcon.CALENDAR_VIEW_MONTH.create()));
        nav.addItem(new SideNavItem(getTranslation("Calculator"), PriceCalculatorView.class, MaterialIcon.CALCULATE.create()));
        nav.addItem(new SideNavItem(getTranslation("view.notifications"), NotificationsView.class, MaterialIcon.WARNING.create()));
        nav.addItem(new SideNavItem(getTranslation("view.about"), AboutView.class, MaterialIcon.INFO.create()));

        addToDrawer(new Div(app, nav), createLinkDiv());
    }

    private Div createLinkDiv() {
        // Ko-Fi link
        final var kofiIcon = new Image("https://storage.ko-fi.com/cdn/cup-border.png", "Ko-fi donations");
        kofiIcon.setWidth("var(--lumo-icon-size-s)");
        final var kofiLink = new Anchor("https://ko-fi.com/F2F4FU50T", getTranslation("kofi.text"));
        kofiLink.addClassNames(
                LumoUtility.AlignItems.CENTER,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.Display.FLEX,
                LumoUtility.FontSize.SMALL,
                LumoUtility.FontWeight.MEDIUM,
                LumoUtility.Gap.SMALL,
                LumoUtility.Height.MEDIUM,
                LumoUtility.JustifyContent.CENTER
        );
        kofiLink.add(kofiIcon);

        // Vaadin link
        final var vaadinIcon = VaadinIcon.VAADIN_H.create();
        vaadinIcon.addClassNames(LumoUtility.IconSize.SMALL);
        final var vaadinLink = new Anchor("http://vaadin.com", getTranslation("Built with Vaadin"));
        vaadinLink.addClassNames(
                LumoUtility.AlignItems.CENTER,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.Display.FLEX,
                LumoUtility.FontSize.SMALL,
                LumoUtility.FontWeight.MEDIUM,
                LumoUtility.Gap.XSMALL,
                LumoUtility.Height.MEDIUM,
                LumoUtility.JustifyContent.CENTER
        );
        vaadinLink.add(vaadinIcon);

        // GitHub link
        final var githubIcon = new Span();
        githubIcon.addClassNames(LumoUtility.Display.FLEX);
        githubIcon.getElement().setProperty("innerHTML", GITHUB_SVG);
        final var githubLink = new Anchor("https://github.com/vesanieminen/ElectricityCostDashboard");
        githubLink.addClassNames(
                LumoUtility.AlignItems.CENTER,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.Display.FLEX,
                LumoUtility.Height.MEDIUM,
                LumoUtility.JustifyContent.CENTER,
                LumoUtility.Width.MEDIUM
        );
        final var githubSpan = new Span(getTranslation("Fork me on GitHub"));
        githubSpan.addClassNames(LumoUtility.Accessibility.SCREEN_READER_ONLY);
        githubLink.add(githubSpan, githubIcon);

        final var discordIcon = new Span();
        discordIcon.addClassNames(LumoUtility.Display.FLEX);
        discordIcon.getElement().setProperty("innerHTML", DISCORD_SVG);
        final var discordLink = new Anchor("https://discord.com/invite/WHcY2UkWVp");
        discordLink.addClassNames(
                LumoUtility.AlignItems.CENTER,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.Display.FLEX,
                LumoUtility.Height.MEDIUM,
                LumoUtility.JustifyContent.CENTER,
                LumoUtility.Width.MEDIUM
        );
        final var discordSpan = new Span(getTranslation("Join Discord"));
        discordSpan.addClassNames(LumoUtility.Accessibility.SCREEN_READER_ONLY);
        discordLink.add(discordIcon);

        var hr = new Hr();
        hr.addClassNames(LumoUtility.Margin.Horizontal.MEDIUM, LumoUtility.Margin.Vertical.XSMALL);

        var row = new Div(githubLink, discordLink);
        row.addClassNames(LumoUtility.Display.FLEX, LumoUtility.JustifyContent.CENTER);

        final var priimaImage = new Image("images/Priima_pks-200x100 (002).png", "PKS Priima");
        priimaImage.addClassNames(LumoUtility.Margin.Vertical.SMALL, LumoUtility.BorderRadius.MEDIUM, LumoUtility.Border.ALL, LumoUtility.BoxShadow.MEDIUM);
        priimaImage.setMaxWidth("100%");
        priimaImage.setWidth("200px");
        priimaImage.getStyle().set("border-color", "#FFFFFF");
        final var priimaAnchor = new Anchor("https://www.pks.fi/sahkotarjoukset/kotiin/sahkotuotteet/priima-alykkaampi-sahko/", priimaImage);
        priimaAnchor.setTarget(AnchorTarget.BLANK);
        priimaAnchor.addClassNames(
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Display.FLEX,
                LumoUtility.JustifyContent.CENTER
        );

        var footer = new Div(kofiLink, vaadinLink, priimaAnchor, hr, row);
        footer.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.Horizontal.SMALL,
                LumoUtility.Padding.Vertical.SMALL
        );
        return footer;
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        title.setText(getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
        return title == null ? "" : getTranslation(title.value().replace(URL_SUFFIX, ""));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        final var priimaImage = new Image("images/Priima_pks-200x100 (002).png", "PKS Priima");
        priimaImage.addClassNames(LumoUtility.Height.MEDIUM);
        final var priimaAnchor = new Anchor("https://www.pks.fi/sahkotarjoukset/kotiin/sahkotuotteet/priima-alykkaampi-sahko/", priimaImage);
        priimaAnchor.addClassNames(LumoUtility.Height.MEDIUM);
        priimaAnchor.setTarget(AnchorTarget.BLANK);
        header.add(priimaAnchor);

        Button theme = new Button(MaterialIcon.DARK_MODE.create());
        theme.getElement().setAttribute("aria-label", getTranslation("Switch to dark mode"));
        theme.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        theme.addClickListener(e -> {
            if (this.darkMode) {
                attachEvent.getUI().getPage().executeJs("document.documentElement.setAttribute('theme', '');");
                theme.setIcon(MaterialIcon.DARK_MODE.create());
                theme.getElement().setAttribute("aria-label", getTranslation("Switch to dark mode"));
            } else {
                attachEvent.getUI().getPage().executeJs("document.documentElement.setAttribute('theme', 'dark');");
                theme.setIcon(MaterialIcon.LIGHT_MODE.create());
                theme.getElement().setAttribute("aria-label", getTranslation("Switch to light mode"));
            }
            this.darkMode = !this.darkMode;
        });
        header.add(theme);

        header.add(createChangeLanguageButton(attachEvent));

        if ("fi".equals(attachEvent.getUI().getLocale().getLanguage())) {
            final var chartOptions = ChartOptions.get(attachEvent.getUI());
            final var lang = new Lang();
            lang.setMonths(new String[]{"Tammikuu", "Helmikuu", "Maaliskuu", "Huhtikuu", "Toukokuu", "Kesäkuu", "Heinäkuu", "Elokuu", "Syyskuu", "Lokakuu", "Marraskuu", "Joulukuu"});
            lang.setShortMonths(new String[]{"Tammi", "Helmi", "Maalis", "Huhti", "Touko", "Kesä", "Heinä", "Elo", "Syys", "Loka", "Marras", "Joulu"});
            lang.setWeekdays(new String[]{"Sunnuntai", "Maanantai", "Tiistai", "Keskiviikko", "Torstai", "Perjantai", "Lauantai"});
            //lang.setRangeSelectorZoom("");
            var decimalFormat = (DecimalFormat) DecimalFormat.getInstance(getLocale());
            lang.setDecimalPoint(decimalFormat.getDecimalFormatSymbols().getDecimalSeparator() + "");
            chartOptions.setLang(lang);
        }

    }

    private Button createChangeLanguageButton(AttachEvent attachEvent) {
        var changeLanguage = new Button();
        changeLanguage.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        updateChangeLanguageButtonIcon(attachEvent.getUI(), changeLanguage);
        changeLanguage.addClickListener(e -> {
            VaadinService.getCurrentResponse().addCookie(new Cookie("locale", fiLocale.equals(attachEvent.getUI().getLocale()) ? enLocale.toLanguageTag() : fiLocale.toLanguageTag()));
            updateChangeLanguageButtonIcon(attachEvent.getUI(), changeLanguage);
            attachEvent.getUI().getPage().reload();
        });
        return changeLanguage;
    }

    private void updateChangeLanguageButtonIcon(UI ui, Button changeLanguage) {
        Image image;
        if (fiLocale.equals(ui.getLocale())) {
            // TODO: Translation
            image = new Image("icons/finnish.png", "Finnish");
            changeLanguage.getElement().setAttribute("aria-label", "Switch to English");
            changeLanguage.getElement().setAttribute("title", "Switch to English");
        } else {
            // TODO: Translation
            image = new Image("icons/english.png", "English");
            changeLanguage.getElement().setAttribute("aria-label", "Switch to Finnish");
            changeLanguage.getElement().setAttribute("title", "Switch to Finnish");
        }
        image.setHeightFull();

        // Wrapper; circle
        Span icon = new Span(image);
        icon.addClassNames(
                LumoUtility.AlignItems.CENTER,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.Display.FLEX,
                LumoUtility.IconSize.MEDIUM,
                LumoUtility.JustifyContent.CENTER,
                LumoUtility.Overflow.HIDDEN
        );
        changeLanguage.setIcon(icon);
    }
}
