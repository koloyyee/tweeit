package co.loyyee;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnvTest {
	@Test
	public void shouldThrowIfNotEnv() {
			assertThrows(IllegalArgumentException.class, () -> Env.get("hello.txt"));
			assertThrows(IllegalArgumentException.class, () -> Env.get("env.txt"));
	}
}
