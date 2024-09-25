package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.froniusvisualizer.services.ObjectMapperService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.vesanieminen.froniusvisualizer.util.Utils.adjustRootFontSize;


@Route(value = "asetukset", layout = MainLayout.class)
@RouteAlias(value = "settings", layout = MainLayout.class)
@PageTitle("Settings")
@Slf4j
public class SettingsView extends Main {

    private final ObjectMapperService mapperService;
    public static final String ZOOM = "settings.zoom";
    private final Select<ZoomLevel> zoomLevelSelect;

    public SettingsView(SettingsState settingsState, ObjectMapperService mapperService) {
        this.mapperService = mapperService;

        addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.MEDIUM,
                LumoUtility.Padding.MEDIUM
        );

        final var electricityCosts = new H3(getTranslation("General"));
        add(electricityCosts);

        zoomLevelSelect = new Select<>();
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
