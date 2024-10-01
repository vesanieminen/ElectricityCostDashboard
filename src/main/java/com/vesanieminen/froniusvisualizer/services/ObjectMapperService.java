package com.vesanieminen.froniusvisualizer.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.server.VaadinSession;
import com.vesanieminen.froniusvisualizer.components.SettingsDialog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Service
@Slf4j
public class ObjectMapperService {

    private final ObjectMapper objectMapper;

    public ObjectMapperService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <C extends AbstractField<C, T>, T> void saveFieldValue(AbstractField<C, T> field) {
        try {
            WebStorage.setItem(field.getId().orElseThrow(), objectMapper.writeValueAsString(field.getValue()));
            VaadinSession.getCurrent().setAttribute(field.getId().orElseThrow(), field.getValue());
        } catch (JsonProcessingException | NoSuchElementException e) {
            log.info("Could not save value: %s".formatted(e.toString()));
        }
    }

    public <C extends HasValue.ValueChangeEvent<T>, T> void readValue(String key, HasValue<C, T> hasValue) {
        if (key == null) {
            return;
        }
        try {
            T value = objectMapper.readValue(key, new TypeReference<>() {
            });
            hasValue.setValue(value);
        } catch (IOException e) {
            log.info("Could not read value: %s".formatted(e.toString()));
        }
    }

    public void readValue(String key, DateTimePicker dateTimePicker) {
        if (key == null) {
            return;
        }
        try {
            var value = objectMapper.readValue(key, new TypeReference<LocalDateTime>() {
            });
            dateTimePicker.setValue(value);
        } catch (IOException e) {
            log.info("Could not read value: %s".formatted(e.toString()));
        }
    }

    public SettingsDialog.ZoomLevel readValue(String key) {
        if (key == null) {
            return null;
        }
        try {
            var value = objectMapper.readValue(key, new TypeReference<SettingsDialog.ZoomLevel>() {
            });
            VaadinSession.getCurrent().setAttribute(SettingsDialog.ZoomLevel.class, value);
            return value;
        } catch (IOException e) {
            log.info("Could not read value: %s".formatted(e.toString()));
        }
        return null;
    }

    public void readAndSetValue(String key, Select<SettingsDialog.ZoomLevel> zoomLevel) {
        if (key == null) {
            return;
        }
        try {
            var value = objectMapper.readValue(key, new TypeReference<SettingsDialog.ZoomLevel>() {
            });
            zoomLevel.setValue(value);
            VaadinSession.getCurrent().setAttribute(key, value);
        } catch (IOException e) {
            log.info("Could not read value: %s".formatted(e.toString()));
        }
    }

    public void readLocalDateTime(String key, DatePicker datePicker) {
        if (key == null) {
            return;
        }
        try {
            var value = objectMapper.readValue(key, new TypeReference<LocalDate>() {
            });
            datePicker.setValue(value);
        } catch (IOException e) {
            log.info("Could not read value: %s".formatted(e.toString()));
        }
    }


}
