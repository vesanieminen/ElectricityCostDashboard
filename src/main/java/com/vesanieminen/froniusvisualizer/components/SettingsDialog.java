package com.vesanieminen.froniusvisualizer.components;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.froniusvisualizer.services.ObjectMapperService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.vesanieminen.froniusvisualizer.util.Utils.adjustRootFontSize;


@Slf4j
@SpringComponent
@RouteScope
public class SettingsDialog extends Dialog {

    private final ObjectMapperService mapperService;
    public static final String ZOOM = "settings.zoom";
    public static final String PRICE_RESOLUTION = "settings.price-resolution";
    private final Select<ZoomLevel> zoomLevelSelect;
    private final Select<PriceResolution> priceResolutionSelect;

    public SettingsDialog(SettingsState settingsState, ObjectMapperService mapperService) {
        this.mapperService = mapperService;

        setHeaderTitle(getTranslation("Settings"));
        final var closeButton = new Button(MaterialIcon.CLOSE.create(), e -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        getHeader().add(closeButton);

        final var generalH3 = new H3(getTranslation("General"));
        add(generalH3);

        priceResolutionSelect = new Select<>();
        priceResolutionSelect.setWidthFull();
        priceResolutionSelect.setId(PRICE_RESOLUTION);
        priceResolutionSelect.setLabel(getTranslation(PRICE_RESOLUTION));
        priceResolutionSelect.setItems(PriceResolution.values());

        priceResolutionSelect.addClassNames(LumoUtility.MaxWidth.SCREEN_SMALL);
        priceResolutionSelect.setHelperText(getTranslation(PRICE_RESOLUTION + ".helper"));
        priceResolutionSelect.setItemLabelGenerator(item -> getTranslation(item.getTranslation()));
        add(priceResolutionSelect);

        zoomLevelSelect = new Select<>();
        zoomLevelSelect.setWidthFull();
        zoomLevelSelect.setId(ZOOM);
        zoomLevelSelect.setLabel(getTranslation("settings.zoom-level"));
        zoomLevelSelect.setItems(ZoomLevel.values());

        zoomLevelSelect.addClassNames(LumoUtility.MaxWidth.SCREEN_SMALL);
        zoomLevelSelect.setHelperText(getTranslation("settings.zoom-level.helper"));
        zoomLevelSelect.setItemLabelGenerator(item -> getTranslation(item.getTranslation()));
        add(zoomLevelSelect);

        final var binder = new Binder<Settings>();
        binder.bind(zoomLevelSelect, Settings::getZoomLevel, Settings::setZoomLevel);
        binder.bind(priceResolutionSelect, Settings::getPriceResolution, Settings::setPriceResolution);
        binder.setBean(settingsState.settings);

        readFieldValues();

        priceResolutionSelect.addValueChangeListener(item -> {
            mapperService.saveFieldValue(priceResolutionSelect);
            if (item != null) {
                UI ui = UI.getCurrent();
                if (ui != null) {
                    //Location current = ui.getInternals().getActiveViewLocation();
                    //ui.navigate(current.getPath());
                    ui.getPage().reload();
                }
            }
        });

        zoomLevelSelect.addValueChangeListener(item -> {
            mapperService.saveFieldValue(zoomLevelSelect);
            if (item != null) {
                adjustRootFontSize(item.getValue().getSize());
            }
        });

        Button themeButton = new Button(MaterialIcon.DARK_MODE.create());
        UI.getCurrent().getPage().executeJs("return document.documentElement.getAttribute('theme');")
                .then(String.class, darkMode -> {
                            if ("dark".equals(darkMode)) {
                                setThemeButtonMode(themeButton, MaterialIcon.LIGHT_MODE, "Switch to light mode");
                            } else {
                                setThemeButtonMode(themeButton, MaterialIcon.DARK_MODE, "Switch to dark mode");
                            }
                        }
                );
        themeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        themeButton.addClickListener(e -> {
            final var ui = UI.getCurrent();
            ui.getPage().executeJs("return document.documentElement.getAttribute('theme');")
                    .then(String.class, darkMode -> {
                                if ("dark".equals(darkMode)) {
                                    ui.getPage().executeJs("document.documentElement.setAttribute('theme', '');");
                                    setThemeButtonMode(themeButton, MaterialIcon.DARK_MODE, "Switch to dark mode");
                                } else {
                                    ui.getPage().executeJs("document.documentElement.setAttribute('theme', 'dark');");
                                    setThemeButtonMode(themeButton, MaterialIcon.LIGHT_MODE, "Switch to light mode");
                                }
                            }
                    );
        });
        add(themeButton);

    }

    private void setThemeButtonMode(Button theme, MaterialIcon darkMode, String translation) {
        theme.setIcon(darkMode.create());
        theme.setText(getTranslation(translation));
        theme.setAriaLabel(getTranslation(translation));
    }

    public void readFieldValues() {
        WebStorage.getItem(zoomLevelSelect.getId().orElseThrow(), item -> mapperService.readAndSetValue(item, zoomLevelSelect));
        WebStorage.getItem(priceResolutionSelect.getId().orElseThrow(), item -> mapperService.readAndSetPriceResolutionValue(item, priceResolutionSelect));
    }

    @Getter
    @AllArgsConstructor
    public enum ZoomLevel {
        XSMALL(80, "zoom-level-xsmall"),
        SMALL(90, "zoom-level-small"),
        MEDIUM(100, "zoom-level-medium"),
        LARGE(125, "zoom-level-large"),
        XLARGE(150, "zoom-level-xlarge"),
        ;
        final double size;
        final String translation;
    }

    @Getter
    @AllArgsConstructor
    public enum PriceResolution {
        HOUR_RESOLUTION("hour-resolution"),
        QUARTER_RESOLUTION("quarter-resolution"),
        ;
        final String translation;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class Settings {
        ZoomLevel zoomLevel;
        PriceResolution priceResolution;
    }

    @VaadinSessionScope
    @Component
    @Getter
    public static class SettingsState {
        Settings settings = new Settings(ZoomLevel.MEDIUM, PriceResolution.QUARTER_RESOLUTION);
    }

}
