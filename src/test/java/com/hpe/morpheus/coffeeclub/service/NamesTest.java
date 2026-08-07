package com.hpe.morpheus.coffeeclub.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Name normalisation")
class NamesTest {

    @ParameterizedTest(name = "\"{0}\" normalises to \"{1}\"")
    @CsvSource({
            "'Bob','Bob'",
            "'  Bob  ','Bob'",
            "'Mary   Jane','Mary Jane'",
            "'O''Brien','O''Brien'"
    })
    void trimsAndCollapsesWhitespace(String raw, String expected) {
        assertThat(Names.normalise(raw)).isEqualTo(expected);
    }

    @Test
    @DisplayName("null in, null out")
    void handlesNull() {
        assertThat(Names.normalise(null)).isNull();
        assertThat(Names.key(null)).isEmpty();
    }

    @Test
    @DisplayName("lookup keys ignore case and surrounding whitespace")
    void keysMatchTheSamePersonRegardlessOfCasing() {
        assertThat(Names.key(" BOB ")).isEqualTo(Names.key("bob"));
        assertThat(Names.key("Mary  Jane")).isEqualTo(Names.key("mary jane"));
        assertThat(Names.key("Bob")).isNotEqualTo(Names.key("Bobby"));
    }
}
