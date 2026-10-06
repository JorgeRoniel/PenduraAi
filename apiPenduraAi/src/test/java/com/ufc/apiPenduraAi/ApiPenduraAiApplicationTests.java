package com.ufc.apiPenduraAi;

import com.ufc.apiPenduraAi.domain.divida.Divida;
import com.ufc.apiPenduraAi.domain.user.User;
import com.ufc.apiPenduraAi.repositories.divida.DividaRepository;
import com.ufc.apiPenduraAi.repositories.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ApiPenduraAiApplicationTests {

	private final WebApplicationContext context;
	private final UserRepository repository;
	private final DividaRepository dividaRepository;
	private final JdbcTemplate jdbcTemplate;

	private MockMvc mock;

	@Autowired
	public ApiPenduraAiApplicationTests(WebApplicationContext context, UserRepository repository, DividaRepository dividaRepository, JdbcTemplate jdbcTemplate){
		this.repository = repository;
		this.context = context;
		this.dividaRepository = dividaRepository;
		this.jdbcTemplate = jdbcTemplate;
	}

	@BeforeEach
	void setUp() {
		mock = webAppContextSetup(context)
				.apply(springSecurity())
				.build();
	}

	@Test
	void unauthenticatedUserCannotListUsers() throws Exception {
		mock.perform(get("/api/user"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void commonUserCannotListUsers() throws Exception {
		mock.perform(get("/api/user")
						.with(user("user@example.com").roles("USER"))
				)
				.andExpect(status().isForbidden());
	}

	@Test
	void adminCanListUsers() throws Exception {
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

	@Test
	void userListLimitsRequestedPageSize() throws Exception{
		mock.perform(
				get("/api/user")
						.param("size", "1000")
						.with(user("admin@example.com").roles("ADMIN"))
		)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.size").value(50));
	}

	@Test
	void userListRejectsForbiddenSortField() throws Exception{
		mock.perform(
				get("/api/user")
						.param("sort", "nome,desc")
						.param("sort", "senha,asc")
						.with(user("admin@mail.com").roles("ADMIN"))
		)
				.andExpect(status().isBadRequest())
				.andExpect(content().string("Campo de ordenação não permitido: senha"));
	}

	@Test
	void databaseContainsDebtQueryIndex(){
		Boolean indexExists = jdbcTemplate.queryForObject(
				"""
					SELECT EXISTS (
						SELECT 1
						FROM pg_indexes
						WHERE schemaname = CURRENT_SCHEMA()
						  AND tablename = 'dividas_tb'
						  AND indexname = 'idx_dividas_user_cliente'
					)
					""",
				Boolean.class
		);

		assertTrue(Boolean.TRUE.equals(indexExists));
	}

	@Test
	void corsAllowsEveryConfiguredOrigin() throws Exception {
		for(String origin : new String[]{
				"http://localhost:4200",
				"http://localhost:4300"
		}) {
			mock.perform(
					options("/api/user/me")
							.header(HttpHeaders.ORIGIN, origin)
							.header(
									HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
									HttpMethod.GET.name()
							)
			)
					.andExpect(status().isOk())
					.andExpect(header().string(
							HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
							origin
					));
		}
	}

	@Test
	void corsRejectsUnknownOrigin() throws Exception {

		mock.perform(
				options("/api/user/me")
						.header(
								HttpHeaders.ORIGIN,
								"https://malicious.com"
						)
						.header(
								HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
								HttpMethod.GET.name()
						)
		)
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(
						HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN
				));
	}

	@Test
	@Transactional
	void postWithoutCsrfTokenIsRejected() throws Exception {
		mock.perform(
				post("/api/user/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
                            {
                              "nome": "Usuário CSRF",
                              "email": "csrf1@mail.com",
                              "senha": "123456"
                            }
                            """)
		)
				.andExpect(status().isForbidden());
	}

	@Test
	@Transactional
	void postWithCsrfTokenIsAccepted() throws Exception {
		mock.perform(
				post("/api/user/register")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
                            {
                              "nome": "Usuário CSRF",
                              "email": "csrf1@mail.com",
                              "senha": "123456"
                            }
                            """)
		)
				.andExpect(status().isCreated());
	}

	// Container Postgres para rodar os testes de integração: Backend <--> BD.
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
