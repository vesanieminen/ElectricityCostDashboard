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
    private final Select<ZoomLevel> zoomLevelSelect;

    public SettingsDialog(SettingsState settingsState, ObjectMapperService mapperService) {
        this.mapperService = mapperService;

        setHeaderTitle(getTranslation("Settings"));
        final var closeButton = new Button(MaterialIcon.CLOSE.create(), e -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        getHeader().add(closeButton);

        final var electricityCosts = new H3(getTranslation("General"));
        add(electricityCosts);

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
        binder.setBean(settingsState.settings);

        readFieldValues();

        zoomLevelSelect.addValueChangeListener(item -> {
            mapperService.saveFieldValue(zoomLevelSelect);
            if (item != null) {
                adjustRootFontSize(item.getValue().getSize());
            }
        });
    }

    private void setThemeButtonMode(Button theme, MaterialIcon darkMode, String translation) {
        theme.setIcon(darkMode.create());
        theme.setText(getTranslation(translation));
        theme.setAriaLabel(getTranslation(translation));
    }

    public void readFieldValues() {
        WebStorage.getItem(zoomLevelSelect.getId().orElseThrow(), item -> mapperService.readAndSetValue(item, zoomLevelSelect));
    }

    @Getter
    @AllArgsConstructor
    public static enum ZoomLevel {
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
    @Setter
    @AllArgsConstructor
    public static class Settings {
        ZoomLevel zoomLevel;
    }

    @VaadinSessionScope
    @Component
    @Getter
    public static class SettingsState {
        Settings settings = new Settings(ZoomLevel.MEDIUM);
    }

}
