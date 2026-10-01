package se.taskapp.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class TaskRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    private Set<String> invalidFields(TaskRequest request) {
        return validator.validate(request)
                .stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("a normal request is valid")
    void normalRequestIsValid() {
        assertThat(invalidFields(new TaskRequest("Buy milk", "2 liters", false))).isEmpty();
    }

    @ParameterizedTest(name = "title [{0}] is rejected")
    @NullSource
    @ValueSource(strings = { "", "   ", "\t", "\n", " ", "​", "　" })
    @DisplayName("titles without visible text are rejected")
    void titleWithoutVisibleTextIsRejected(String title) {
        assertThat(invalidFields(new TaskRequest(title, null, false))).containsExactly("title");
    }

    @ParameterizedTest(name = "title [{0}] is rejected")
    @ValueSource(strings = { "Buy\u0000milk", "Buy\nmilk", "Buy\tmilk", "Buy\u001Bmilk" })
    @DisplayName("titles with control characters are rejected")
    void titleWithControlCharactersIsRejected(String title) {
        assertThat(invalidFields(new TaskRequest(title, null, false))).containsExactly("title");
    }

    @ParameterizedTest(name = "title [{0}] is accepted")
    @ValueSource(strings = {
            "Köp mjölk",
            "🙂",
            "👨‍👩‍👧",
            "42",
            "!",
            "<script>alert(1)</script>",
            "Robert'); DROP TABLE tasks;--" })
    @DisplayName("unusual but legitimate titles are accepted as plain text")
    void unusualButLegitimateTitleIsAccepted(String title) {
        assertThat(invalidFields(new TaskRequest(title, null, false))).isEmpty();
    }

    @Test
    @DisplayName("a title of exactly 255 characters is accepted")
    void titleAtMaximumLengthIsAccepted() {
        assertThat(invalidFields(new TaskRequest("a".repeat(255), null, false))).isEmpty();
    }

    @Test
    @DisplayName("a title of 256 characters is rejected")
    void titleOverMaximumLengthIsRejected() {
        assertThat(invalidFields(new TaskRequest("a".repeat(256), null, false))).containsExactly("title");
    }

    @Test
    @DisplayName("surrounding whitespace is removed before the length is checked")
    void titleIsStrippedBeforeLengthCheck() {
        TaskRequest request = new TaskRequest("     " + "a".repeat(255) + "     ", null, false);

        assertThat(request.title()).hasSize(255);
        assertThat(invalidFields(request)).isEmpty();
    }

    @Test
    @DisplayName("surrounding whitespace is removed from the title")
    void titleIsStripped() {
        assertThat(new TaskRequest("   Buy milk   ", null, false).title()).isEqualTo("Buy milk");
    }

    @ParameterizedTest(name = "description [{0}] becomes null")
    @NullSource
    @ValueSource(strings = { "", "   ", "\n\t" })
    @DisplayName("a blank description is stored as no description")
    void blankDescriptionBecomesNull(String description) {
        TaskRequest request = new TaskRequest("Buy milk", description, false);

        assertThat(request.description()).isNull();
        assertThat(invalidFields(request)).isEmpty();
    }

    @Test
    @DisplayName("a description may span several lines")
    void multilineDescriptionIsAccepted() {
        TaskRequest request = new TaskRequest("Buy milk", "line one\nline two\r\n\tindented", false);

        assertThat(invalidFields(request)).isEmpty();
    }

    @Test
    @DisplayName("a description with a null byte is rejected")
    void descriptionWithNullByteIsRejected() {
        assertThat(invalidFields(new TaskRequest("Buy milk", "bad\u0000byte", false)))
                .containsExactly("description");
    }

    @Test
    @DisplayName("a description of exactly 1000 characters is accepted")
    void descriptionAtMaximumLengthIsAccepted() {
        assertThat(invalidFields(new TaskRequest("Buy milk", "a".repeat(1000), false))).isEmpty();
    }

    @Test
    @DisplayName("a description of 1001 characters is rejected")
    void descriptionOverMaximumLengthIsRejected() {
        assertThat(invalidFields(new TaskRequest("Buy milk", "a".repeat(1001), false)))
                .containsExactly("description");
    }
}