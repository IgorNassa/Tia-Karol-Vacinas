package br.com.tiakarol.api.migration;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class LegacyValueNormalizer {
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT)
    );

    private LegacyValueNormalizer() {
    }

    static String digits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    static String canonicalText(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value.strip(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    static boolean validCpf(String value) {
        String cpf = digits(value);
        if (cpf.length() != 11 || cpf.chars().distinct().count() == 1) return false;
        int first = cpfDigit(cpf, 9, 10);
        int second = cpfDigit(cpf, 10, 11);
        return first == Character.digit(cpf.charAt(9), 10)
                && second == Character.digit(cpf.charAt(10), 10);
    }

    private static int cpfDigit(String cpf, int length, int weight) {
        int sum = 0;
        for (int index = 0; index < length; index++) {
            sum += Character.digit(cpf.charAt(index), 10) * (weight - index);
        }
        int remainder = 11 - (sum % 11);
        return remainder >= 10 ? 0 : remainder;
    }

    static Optional<LocalDate> date(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        String candidate = value.strip();
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return Optional.of(LocalDate.parse(candidate, formatter));
            } catch (DateTimeParseException ignored) {
                // Tenta o próximo formato conhecido do sistema legado.
            }
        }
        return Optional.empty();
    }
}
