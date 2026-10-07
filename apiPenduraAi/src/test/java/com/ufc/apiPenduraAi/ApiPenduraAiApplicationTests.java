package com.ufc.apiPenduraAi;

import com.ufc.apiPenduraAi.domain.divida.Divida;
import com.ufc.apiPenduraAi.domain.refresh.RefreshSession;
import com.ufc.apiPenduraAi.domain.user.User;
import com.ufc.apiPenduraAi.exceptions.token.InvalidTokenException;
import com.ufc.apiPenduraAi.repositories.divida.DividaRepository;
import com.ufc.apiPenduraAi.repositories.refresh.RefreshSessionRepository;
import com.ufc.apiPenduraAi.repositories.user.UserRepository;
import com.ufc.apiPenduraAi.services.refresh.RefreshSessionService;
import com.ufc.apiPenduraAi.services.refresh.RefreshTokenCodec;
import com.ufc.apiPenduraAi.services.refresh.implementation.RefreshSessionCleanupService;
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
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

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
	private final RefreshSessionRepository refreshSessionRepository;
	private final RefreshSessionService refreshSessionService;
	private final RefreshTokenCodec refreshTokenCodec;
	private final RefreshSessionCleanupService refreshSessionCleanupService;

	private MockMvc mock;

	@Autowired
	public ApiPenduraAiApplicationTests(
			WebApplicationContext context,
			UserRepository repository,
			DividaRepository dividaRepository,
			JdbcTemplate jdbcTemplate,
			RefreshSessionRepository refreshSessionRepository,
			RefreshSessionService refreshSessionService,
			RefreshTokenCodec tokenCodec,
			RefreshSessionCleanupService refreshSessionCleanupService
	){
		this.repository = repository;
		this.context = context;
		this.dividaRepository = dividaRepository;
		this.jdbcTemplate = jdbcTemplate;
		this.refreshSessionRepository = refreshSessionRepository;
		this.refreshSessionService = refreshSessionService;
		this.refreshTokenCodec = tokenCodec;
		this.refreshSessionCleanupService = refreshSessionCleanupService;
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
	void databaseContainsRefreshSessionTable() {
		Boolean tableExists = jdbcTemplate.queryForObject(
				"""
            SELECT EXISTS (
                SELECT 1
                FROM information_schema.tables
                WHERE table_schema = CURRENT_SCHEMA()
                  AND table_name = 'refresh_session_tb'
            )
            """,
				Boolean.class
		);

		assertTrue(Boolean.TRUE.equals(tableExists));
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

	@Test
	@Transactional
	void persistsRefreshSessionWithUuid(){
		User user = repository.saveAndFlush(
				new User(
						"user de sessão",
						"session@mail.com",
						"encoded-password"
				)
		);

		UUID sessionId = UUID.randomUUID();
		String tokenHash = "a".repeat(64);
		Instant expiration = Instant.now().plus(7, ChronoUnit.DAYS);

		RefreshSession session = new RefreshSession(sessionId, user, tokenHash, expiration);

		refreshSessionRepository.saveAndFlush(session);

		RefreshSession savedSession = refreshSessionRepository.findById(sessionId).orElseThrow();

		assertEquals(sessionId, savedSession.getId());
		assertEquals(user.getId(), savedSession.getUser().getId());
		assertEquals(tokenHash, savedSession.getTokenHash());
		assertEquals(expiration, savedSession.getExpiresAt());
		assertNull(savedSession.getRevokedAt());
		assertNotNull(savedSession.getCreatedAt());
		assertNotNull(savedSession.getUpdatedAt());
		assertTrue(savedSession.isActive(Instant.now()));
	}

	@Test
	@Transactional
	void createsRefreshSessionWithoutStoringRawToken() {
		User user = repository.saveAndFlush(
				new User(
						"Usuário refresh",
						"refresh@mail.com",
						"encoded-password"
				)
		);

		String rawToken = refreshSessionService.createSession(user);
		var parsedToken = refreshTokenCodec.parse(rawToken);

		RefreshSession savedSession = refreshSessionRepository
				.findById(parsedToken.sessionId())
				.orElseThrow();

		assertEquals(user.getId(), savedSession.getUser().getId());

		assertEquals(
				parsedToken.secretHash(),
				savedSession.getTokenHash()
		);

		assertNotEquals(rawToken, savedSession.getTokenHash());
		assertNull(savedSession.getRevokedAt());
		assertTrue(savedSession.isActive(Instant.now()));

		assertTrue(
				savedSession.getExpiresAt()
						.isAfter(Instant.now().plus(6, ChronoUnit.DAYS))
		);

		assertTrue(
				savedSession.getExpiresAt()
						.isBefore(Instant.now().plus(8, ChronoUnit.DAYS))
		);
	}

	@Test
	@Transactional
	void rotatesRefreshTokenUsingTheSameSession() {
		User user = repository.saveAndFlush(
				new User("Usuário rotação", "rotate@mail.com", "encoded-password")
		);

		String firstToken = refreshSessionService.createSession(user);
		var firstParsed = refreshTokenCodec.parse(firstToken);

		var rotation = refreshSessionService.rotateSession(firstToken);
		var secondParsed = refreshTokenCodec.parse(rotation.refreshToken());

		assertEquals(firstParsed.sessionId(), secondParsed.sessionId());
		assertNotEquals(firstToken, rotation.refreshToken());
		assertNotEquals(firstParsed.secretHash(), secondParsed.secretHash());
		assertEquals(user.getId(), rotation.user().getId());

		RefreshSession savedSession = refreshSessionRepository
				.findById(firstParsed.sessionId())
				.orElseThrow();

		assertEquals(secondParsed.secretHash(), savedSession.getTokenHash());
		assertNull(savedSession.getRevokedAt());
	}

	@Test
	@Transactional
	void reusingConsumedRefreshTokenRevokesSession() {
		User user = repository.saveAndFlush(
				new User("Usuário reutilização", "reuse@mail.com", "encoded-password")
		);

		String firstToken = refreshSessionService.createSession(user);
		var firstParsed = refreshTokenCodec.parse(firstToken);
		var rotation = refreshSessionService.rotateSession(firstToken);

		assertThrows(
				InvalidTokenException.class,
				() -> refreshSessionService.rotateSession(firstToken)
		);

		RefreshSession savedSession = refreshSessionRepository
				.findById(firstParsed.sessionId())
				.orElseThrow();

		assertNotNull(savedSession.getRevokedAt());
		assertFalse(savedSession.isActive(Instant.now()));
		assertThrows(
				InvalidTokenException.class,
				() -> refreshSessionService.rotateSession(rotation.refreshToken())
		);
	}

	@Test
	@Transactional
	void logoutRevokesRefreshSession() {
		User user = repository.saveAndFlush(
				new User("Usuário logout", "logout@mail.com", "encoded-password")
		);

		String refreshToken = refreshSessionService.createSession(user);
		var parsedToken = refreshTokenCodec.parse(refreshToken);

		refreshSessionService.revokeSession(refreshToken);

		RefreshSession savedSession = refreshSessionRepository
				.findById(parsedToken.sessionId())
				.orElseThrow();

		assertNotNull(savedSession.getRevokedAt());
		assertFalse(savedSession.isActive(Instant.now()));
	}

	@Test
	@Transactional
	void cleanupDeletesOldRevokedSessions() {
		User user = repository.saveAndFlush(
				new User("Usuário limpeza", "cleanup@mail.com", "encoded-password")
		);
		UUID sessionId = UUID.randomUUID();
		Instant now = Instant.now();

		jdbcTemplate.update(
				"""
				INSERT INTO refresh_session_tb (
				    id, user_id, token_hash, expires_at,
				    revoked_at, created_at, updated_at
				) VALUES (?, ?, ?, ?, ?, ?, ?)
				""",
				sessionId,
				user.getId(),
				"b".repeat(64),
				OffsetDateTime.ofInstant(now.plus(1, ChronoUnit.DAYS), ZoneOffset.UTC),
				OffsetDateTime.ofInstant(now.minus(31, ChronoUnit.DAYS), ZoneOffset.UTC),
				OffsetDateTime.ofInstant(now.minus(40, ChronoUnit.DAYS), ZoneOffset.UTC),
				OffsetDateTime.ofInstant(now.minus(31, ChronoUnit.DAYS), ZoneOffset.UTC)
		);

		assertEquals(1, refreshSessionCleanupService.deleteObsoleteSessions());
		assertEquals(
				0,
				jdbcTemplate.queryForObject(
						"SELECT COUNT(*) FROM refresh_session_tb WHERE id = ?",
						Integer.class,
						sessionId
				)
		);
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
