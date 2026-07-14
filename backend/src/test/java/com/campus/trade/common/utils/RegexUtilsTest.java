package com.campus.trade.common.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegexUtilsTest {
    @Test
    void validatesChineseMobileNumbers() {
        assertThat(RegexUtils.isPhone("13800000000")).isTrue();
        assertThat(RegexUtils.isPhone("12800000000")).isFalse();
        assertThat(RegexUtils.isPhone("1380000000")).isFalse();
    }

    @Test
    void validatesStudentIds() {
        assertThat(RegexUtils.isStudentId("20210001")).isTrue();
        assertThat(RegexUtils.isStudentId("A20210001")).isTrue();
        assertThat(RegexUtils.isStudentId("123")).isFalse();
    }
}
