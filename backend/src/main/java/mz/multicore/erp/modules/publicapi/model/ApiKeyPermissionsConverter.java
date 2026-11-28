package mz.multicore.erp.modules.publicapi.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Stores the permissions of a key as a comma-separated list of enum names, in declaration order. */
@Converter
public class ApiKeyPermissionsConverter implements AttributeConverter<Set<ApiKeyPermission>, String> {

    @Override
    public String convertToDatabaseColumn(Set<ApiKeyPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return "";
        }
        return EnumSet.copyOf(permissions).stream().map(Enum::name).collect(Collectors.joining(","));
    }

    @Override
    public Set<ApiKeyPermission> convertToEntityAttribute(String column) {
        EnumSet<ApiKeyPermission> permissions = EnumSet.noneOf(ApiKeyPermission.class);
        if (column != null) {
            Arrays.stream(column.split(","))
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    .map(ApiKeyPermission::valueOf)
                    .forEach(permissions::add);
        }
        return permissions;
    }
}
