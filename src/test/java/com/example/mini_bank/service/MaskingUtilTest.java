package com.example.mini_bank.service;

import org.junit.jupiter.api.Test;

import com.example.mini_bank.util.MaskingUtil;

import static org.assertj.core.api.Assertions.assertThat;

public class MaskingUtilTest {
	
	@Test
    void maskCardNumber_shouldMaskProperly() {
        
        String card = "1234567812345678";
        String masked = MaskingUtil.maskCardNumber(card);
        assertThat(masked).isEqualTo("**** **** **** 5678");

        
        String shortCard = "123";
        assertThat(MaskingUtil.maskCardNumber(shortCard)).isEqualTo("****");

        // null
        assertThat(MaskingUtil.maskCardNumber(null)).isEqualTo("****");
    }

    @Test
    void maskEmail_shouldMaskProperly() {

        String email = "alex@example.com";
        String masked = MaskingUtil.maskEmail(email);
        assertThat(masked).isEqualTo("al****@example.com");

        String shortEmail = "a@domain.com";
        assertThat(MaskingUtil.maskEmail(shortEmail)).isEqualTo("a****@domain.com");

        String twoChars = "ab@domain.com";
        assertThat(MaskingUtil.maskEmail(twoChars)).isEqualTo("a****@domain.com");

        assertThat(MaskingUtil.maskEmail("invalid")).isEqualTo("****");

        // null
        assertThat(MaskingUtil.maskEmail(null)).isEqualTo("****");
    }
}
