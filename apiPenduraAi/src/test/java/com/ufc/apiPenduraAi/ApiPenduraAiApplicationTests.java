package com.ufc.apiPenduraAi;

import com.ufc.apiPenduraAi.domain.divida.Divida;
import com.ufc.apiPenduraAi.domain.user.User;
import com.ufc.apiPenduraAi.repositories.divida.DividaRepository;
import com.ufc.apiPenduraAi.repositories.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ApiPenduraAiApplicationTests {

	private final WebApplicationContext context;
	private final UserRepository repository;
	private final DividaRepository dividaRepository;
	private MockMvc mock;

	@Autowired
	public ApiPenduraAiApplicationTests(WebApplicationContext context, UserRepository repository, DividaRepository dividaRepository){
		this.repository = repository;
		this.context = context;
		this.dividaRepository = dividaRepository;
	}

	@BeforeEach
	void setUp() {
		mock = webAppContextSetup(context)
				.apply(springSecurity())
				.build();
	}

	@Test
	void unauthenticateUserCannotListUsers() throws Exception {
		mock.perform(get("/api/user"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void CommonUserCannotListUsers() throws Exception {
		mock.perform(get("/api/user")
						.with(user("user@example.com").roles("USER"))
				)
				.andExpect(status().isForbidden());
	}

	@Test
	void authenticateUserCanListUsers() throws Exception {
		mock.perform(get("/api/user")
						.with(user("user@example.com").roles("ADMIN"))
				)
				.andExpect(status().isOk());
	}

	@Test
	@Transactional // Usando transação pois ao final, o spring faz rollback para não poluir o BD do container.
	void databaseRejectsEmailsThatDifferOnlyByCase() {
		User user1 = new User(
				"First",
				"f@dev.com",
				"encoded-password"
		);

		// Usando saveAndFlush() pois ele força que o INSERT seja realizado imediatamente
		// O save() poderia fazer com que o INSERT fosse realizado só no final da transação
		repository.saveAndFlush(user1);

		User user2 = new User(
				"Second",
				"F@dev.Com",
				"encoded-password"
		);

		assertThrows(
				DataIntegrityViolationException.class,
				() -> repository.saveAndFlush(user2)
		);
	}

	@Test
	@Transactional
	void databaseAcceptsMinimumDebtValue(){
		User owner = new User("usuario", "user@mail.com", "encoded-password");
		repository.saveAndFlush(owner);

		Divida divida = dividaRepository.saveAndFlush(
				new Divida(
						"cliente",
						new BigDecimal("0.01"),
						owner
				)
		);

		assertNotNull(divida.getId());


	}

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres =
			new PostgreSQLContainer("postgres:17-alpine")
					.withDatabaseName("pendura_ai_test")
					.withUsername("test")
					.withPassword("test");

	@Test
	void contextLoads() {
	}

}
