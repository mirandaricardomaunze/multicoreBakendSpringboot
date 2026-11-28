package mz.multicore.erp.modules.webhooks.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Stores the chosen events as a comma-separated list of wire codes, in declaration order. */
@Converter
public class WebhookEventsConverter implements AttributeConverter<Set<WebhookEventType>, String> {

    @Override
    public String convertToDatabaseColumn(Set<WebhookEventType> events) {
        if (events == null || events.isEmpty()) {
            return "";
        }
        return EnumSet.copyOf(events).stream().map(WebhookEventType::code).collect(Collectors.joining(","));
    }

    @Override
    public Set<WebhookEventType> convertToEntityAttribute(String column) {
        EnumSet<WebhookEventType> events = EnumSet.noneOf(WebhookEventType.class);
        if (column != null) {
            Arrays.stream(column.split(","))
                    .map(WebhookEventType::fromCode)
                    .flatMap(Optional::stream)
                    .forEach(events::add);
        }
        return events;
    }
}
