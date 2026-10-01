package se.taskapp.dto;

public final class ValidationPatterns {

    public static final String VISIBLE_TEXT =
            "[^\\p{Cc}]*[\\p{L}\\p{N}\\p{P}\\p{S}][^\\p{Cc}]*";

    public static final String NO_CONTROL_CHARACTERS =
            "(?:[^\\p{Cc}]|[\\n\\r\\t])*";

    private ValidationPatterns() {
    }
}