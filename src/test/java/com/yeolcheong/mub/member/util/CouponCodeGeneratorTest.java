package com.yeolcheong.mub.member.util;

import static org.assertj.core.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CouponCodeGenerator")
class CouponCodeGeneratorTest {

	@Test
	@DisplayName("should generate code with given prefix and 16-char random suffix")
	void shouldGenerateCodeWithPrefixAndRandomSuffix() {
		// when
		String code = CouponCodeGenerator.generate("MUB");

		// then
		assertThat(code).startsWith("MUB-");
		assertThat(code).hasSize("MUB-".length() + 16);
		String suffix = code.substring("MUB-".length());
		assertThat(suffix).matches("[0-9A-F]{16}");
	}

	@Test
	@DisplayName("should generate unique codes across repeated calls")
	void shouldGenerateUniqueCodesAcrossRepeatedCalls() {
		// given
		Set<String> codes = new HashSet<>();
		int iterations = 1000;

		// when
		for (int i = 0; i < iterations; i++) {
			codes.add(CouponCodeGenerator.generate("MUB"));
		}

		// then
		assertThat(codes).hasSize(iterations);
	}

	@Test
	@DisplayName("should honor a different prefix")
	void shouldHonorDifferentPrefix() {
		// when
		String code = CouponCodeGenerator.generate("WELCOME");

		// then
		assertThat(code).startsWith("WELCOME-");
	}
}
