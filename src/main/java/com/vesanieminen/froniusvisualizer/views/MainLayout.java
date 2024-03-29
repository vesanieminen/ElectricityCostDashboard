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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.froniusvisualizer.components.MaterialIcon;
import com.vesanieminen.froniusvisualizer.util.css.FontFamily;
import jakarta.servlet.http.Cookie;

import java.text.DecimalFormat;
import java.util.Arrays;

import static com.vesanieminen.froniusvisualizer.util.Utils.enLocale;
import static com.vesanieminen.froniusvisualizer.util.Utils.fiLocale;

public class MainLayout extends AppLayout implements BeforeEnterObserver {

    public static final String URL_SUFFIX = " ⋅ Liukuri";

    public boolean darkMode = false;

    public static final String DISCORD_SVG = "<svg class=\"icon-s\" fill=\"var(--lumo-primary-text-color)\" xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 127.14 96.36\"><g id=\"图层_2\" data-name=\"图层 2\"><g id=\"Discord_Logos\" data-name=\"Discord Logos\"><g id=\"Discord_Logo_-_Large_-_White\" data-name=\"Discord Logo - Large - White\"><path d=\"M107.7,8.07A105.15,105.15,0,0,0,81.47,0a72.06,72.06,0,0,0-3.36,6.83A97.68,97.68,0,0,0,49,6.83,72.37,72.37,0,0,0,45.64,0,105.89,105.89,0,0,0,19.39,8.09C2.79,32.65-1.71,56.6.54,80.21h0A105.73,105.73,0,0,0,32.71,96.36,77.7,77.7,0,0,0,39.6,85.25a68.42,68.42,0,0,1-10.85-5.18c.91-.66,1.8-1.34,2.66-2a75.57,75.57,0,0,0,64.32,0c.87.71,1.76,1.39,2.66,2a68.68,68.68,0,0,1-10.87,5.19,77,77,0,0,0,6.89,11.1A105.25,105.25,0,0,0,126.6,80.22h0C129.24,52.84,122.09,29.11,107.7,8.07ZM42.45,65.69C36.18,65.69,31,60,31,53s5-12.74,11.43-12.74S54,46,53.89,53,48.84,65.69,42.45,65.69Zm42.24,0C78.41,65.69,73.25,60,73.25,53s5-12.74,11.44-12.74S96.23,46,96.12,53,91.08,65.69,84.69,65.69Z\"/></g></g></g></svg>";
    public static final String GITHUB_SVG = "<svg class=\"icon-s\" viewBox=\"0 0 98 96\" xmlns=\"http://www.w3.org/2000/svg\"><path fill-rule=\"evenodd\" clip-rule=\"evenodd\" d=\"M48.854 0C21.839 0 0 22 0 49.217c0 21.756 13.993 40.172 33.405 46.69 2.427.49 3.316-1.059 3.316-2.362 0-1.141-.08-5.052-.08-9.127-13.59 2.934-16.42-5.867-16.42-5.867-2.184-5.704-5.42-7.17-5.42-7.17-4.448-3.015.324-3.015.324-3.015 4.934.326 7.523 5.052 7.523 5.052 4.367 7.496 11.404 5.378 14.235 4.074.404-3.178 1.699-5.378 3.074-6.6-10.839-1.141-22.243-5.378-22.243-24.283 0-5.378 1.94-9.778 5.014-13.2-.485-1.222-2.184-6.275.486-13.038 0 0 4.125-1.304 13.426 5.052a46.97 46.97 0 0 1 12.214-1.63c4.125 0 8.33.571 12.213 1.63 9.302-6.356 13.427-5.052 13.427-5.052 2.67 6.763.97 11.816.485 13.038 3.155 3.422 5.015 7.822 5.015 13.2 0 18.905-11.404 23.06-22.324 24.283 1.78 1.548 3.316 4.481 3.316 9.126 0 6.6-.08 11.897-.08 13.526 0 1.304.89 2.853 3.316 2.364 19.412-6.52 33.405-24.935 33.405-46.691C97.707 22 75.788 0 48.854 0z\" fill=\"var(--lumo-primary-text-color)\"/></svg>";
    public static final String UPCLOUD_SVG = "<svg class=\"icon-s color-upcloud \" width=\"164\" height=\"26\" viewBox=\"0 0 164 26\" fill=\"none\" xmlns=\"http://www.w3.org/2000/svg\"><path d=\"M59.035 14.67V1.096h2.528v13.318c0 3.413 1.936 4.871 5.248 4.871 3.167 0 5.087-1.458 5.087-4.871V1.096h2.512V14.67c0 4.856-3.392 6.795-7.695 6.795-4.368 0-7.68-1.94-7.68-6.795zm26.111-8.35c-2.096 0-3.568.898-4.656 2.388h-.064v-2.05h-2.272v19.15h2.272v-6.362h.064c1.2 1.538 2.608 2.131 4.576 2.131 3.936 0 6.32-2.965 6.32-7.628 0-4.824-2.56-7.628-6.24-7.628zm-.256 13.334c-3.344 0-4.608-2.548-4.608-5.69 0-3.14 1.472-5.72 4.64-5.72 2.752 0 4.128 2.468 4.128 5.72 0 3.286-1.376 5.706-4.16 5.69zm8.912-8.542c0-5.897 3.535-10.465 9.519-10.465 4.688 0 7.52 2.693 8.064 6.395h-2.48c-.48-2.5-2.464-4.215-5.648-4.215-4.416 0-6.88 3.558-6.88 8.27 0 4.903 2.72 8.22 6.912 8.22 3.792 0 5.584-2.58 5.84-5.528h2.496c-.032 1.987-.992 4.23-2.416 5.64-1.408 1.379-3.456 2.1-6.016 2.1-5.696.016-9.391-4.327-9.391-10.417zm23.119-10.016h-2.272v20.048h2.272V1.096zm9.775 5.193c-4.336 0-7.024 3.413-7.024 7.628 0 4.199 2.688 7.628 7.024 7.628 4.352 0 6.992-3.414 6.992-7.628 0-4.215-2.64-7.628-6.992-7.628zm0 13.349c-3.088 0-4.688-2.516-4.688-5.721s1.6-5.753 4.688-5.753c3.088 0 4.656 2.548 4.656 5.753 0 3.189-1.568 5.72-4.656 5.72zm18.928 1.506v-1.987h-.064c-1.04 1.41-2.272 2.324-4.384 2.324-2.976 0-4.88-1.795-4.88-4.792V6.657h2.272v9.984c0 1.875 1.2 2.98 3.264 2.98 2.304 0 3.792-1.73 3.792-4.038V6.657h2.271V21.16l-2.271-.016zm16.015-20.048v7.612h-.048a5.332 5.332 0 00-4.544-2.387c-3.584 0-6.368 2.804-6.368 7.628.016 4.647 2.48 7.628 6.416 7.612 2.048 0 3.408-.77 4.496-2.18h.048v1.763h2.272V1.096h-2.272zm-4.448 18.558c-2.784 0-4.192-2.404-4.192-5.69 0-3.252 1.408-5.72 4.16-5.72 3.008 0 4.64 2.355 4.64 5.72 0 3.286-1.36 5.706-4.608 5.69z\" fill=\"#000\"></path><path d=\"M47.131 13.26a3.953 3.953 0 013.952 3.958 3.964 3.964 0 01-3.952 3.958H18.844V10.888H20.7v8.446h26.447a2.112 2.112 0 002.112-2.116c0-1.17-.944-2.115-2.112-2.115H22.54V13.26h24.591z\" fill=\"currentColor\"></path><path d=\"M28.892 1.112H18.844v4.231H20.7V2.955h8.192c1.168 0 2.112.946 2.112 2.116 0 1.17-.944 2.115-2.112 2.115H4.541a3.953 3.953 0 00-3.952 3.958 3.964 3.964 0 003.952 3.959h12.431V13.26H4.542a2.112 2.112 0 01-2.112-2.116c0-1.17.944-2.115 2.112-2.115h24.335a3.953 3.953 0 003.952-3.958c0-2.18-1.776-3.959-3.936-3.959z\" fill=\"currentColor\"></path></svg>";

    private final Header header;
    private final H1 title;
    private boolean isLiukuriVideoAdShown;
    private Anchor upcloudLink;

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
        nav.addItem(new SideNavItem(getTranslation("view.videos"), HelpView.class, MaterialIcon.QUESTION_MARK.create()));
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

        // UpCloud link
        final var upcloudIcon = new Image("images/upcloud_logo_icon_purple-e1542796638720-1.png", getTranslation("upcloud.icon"));
        upcloudIcon.setWidth("var(--lumo-icon-size-l)");
        upcloudLink = new Anchor("https://upcloud.com/", getTranslation("upcloud.ad"));
        upcloudLink.addClassNames(
                LumoUtility.AlignItems.CENTER,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.Display.FLEX,
                LumoUtility.FontSize.SMALL,
                LumoUtility.FontWeight.MEDIUM,
                LumoUtility.Gap.SMALL,
                LumoUtility.Height.MEDIUM,
                LumoUtility.JustifyContent.CENTER
        );
        upcloudLink.add(upcloudIcon);
        upcloudLink.setVisible(false);

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

        var footer = new Div(kofiLink, vaadinLink, upcloudLink, hr, row);

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
            lang.setRangeSelectorZoom("Aikaväli");
            var decimalFormat = (DecimalFormat) DecimalFormat.getInstance(getLocale());
            lang.setDecimalPoint(decimalFormat.getDecimalFormatSymbols().getDecimalSeparator() + "");
            chartOptions.setLang(lang);
        }

        //if (!isLiukuriVideoAdShown) {
        //    isLiukuriVideoAdShown = true;
        //    final var dialog = new Dialog();
        //    final var routerLink = new RouterLink(getTranslation("help.ad"), HelpView.class);
        //    dialog.add(routerLink);
        //    dialog.setHeaderTitle("User details");

        //    Button closeButton = new Button(MaterialIcon.CLOSE.create(), (e) -> dialog.close());
        //    closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        //    dialog.getHeader().add(closeButton);
        //    dialog.open();
        //}

        String url = ((VaadinServletRequest) VaadinService.getCurrentRequest()).getServerName();
        final var upcloudDomains = Arrays.asList("94.237.37.229", "94-237-37-229.fi-hel1.upcloud.host");
        upcloudLink.setVisible((upcloudDomains.contains(url)));
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
            changeLanguage.setAriaLabel("Switch to English");
        } else {
            // TODO: Translation
            image = new Image("icons/english.png", "English");
            changeLanguage.setAriaLabel("Switch to Finnish");
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


    public void setParameter(BeforeEvent beforeEvent, @OptionalParameter String parameter) {
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    }

}
