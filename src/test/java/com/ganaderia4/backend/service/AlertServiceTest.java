package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.AlertResponseDTO;
import com.ganaderia4.backend.dto.AlertUpdateRequestDTO;
import com.ganaderia4.backend.config.PaginationProperties;
import com.ganaderia4.backend.exception.BadRequestException;
import com.ganaderia4.backend.model.Alert;
import com.ganaderia4.backend.model.AlertStatus;
import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.CollarStatus;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.Location;
import com.ganaderia4.backend.notification.NotificationDispatcher;
import com.ganaderia4.backend.notification.NotificationMessage;
import com.ganaderia4.backend.observability.DomainMetricsService;
import com.ganaderia4.backend.pattern.factory.alert.AlertFactory;
import com.ganaderia4.backend.repository.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AlertFactory alertFactory;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private DomainMetricsService domainMetricsService;

    @Mock
    private NotificationDispatcher notificationDispatcher;

    @Mock
    private AlertPriorityScorer alertPriorityScorer;

    @Spy
    private PaginationService paginationService = new PaginationService(new PaginationProperties());

    @InjectMocks
    private AlertService alertService;

    private Cow cow;
    private Location location;
    private AlertPriorityScorer.AlertPriorityScoringContext scoringContext;

    @BeforeEach
    void setUp() {
        cow = new Cow();
        cow.setId(1L);
        cow.setToken("VACA-001");
        cow.setName("Luna");

        location = new Location();
        location.setId(10L);
        location.setCow(cow);
        location.setTimestamp(LocalDateTime.now());

        scoringContext = AlertPriorityScorer.AlertPriorityScoringContext.empty(LocalDateTime.now());
        lenient().when(alertPriorityScorer.buildContext(anyList())).thenReturn(scoringContext);
    }

    @Test
    void shouldCreateExitGeofenceAlertWhenNoPendingAlertExists() {
        Alert alertToCreate = new Alert();
        alertToCreate.setType(AlertType.EXIT_GEOFENCE);
        alertToCreate.setStatus(AlertStatus.PENDIENTE);
        alertToCreate.setCow(cow);
        alertToCreate.setLocation(location);
        alertToCreate.setMessage("La vaca VACA-001 salió de la geocerca activa");
        alertToCreate.setCreatedAt(LocalDateTime.now());

        when(alertRepository.findByCowAndTypeAndStatus(
                cow,
                AlertType.EXIT_GEOFENCE,
                AlertStatus.PENDIENTE
        )).thenReturn(Optional.empty());

        when(alertFactory.createAlert(AlertType.EXIT_GEOFENCE, cow, location)).thenReturn(alertToCreate);

        when(alertRepository.save(alertToCreate)).thenAnswer(invocation -> {
            Alert alert = invocation.getArgument(0);
            alert.setId(100L);
            return alert;
        });

        Alert created = alertService.createExitGeofenceAlert(cow, location);

        assertNotNull(created);
        assertEquals(AlertType.EXIT_GEOFENCE, created.getType());
        assertEquals(AlertStatus.PENDIENTE, created.getStatus());
        assertEquals(cow, created.getCow());
        assertEquals(location, created.getLocation());
        assertTrue(created.getMessage().contains("VACA-001"));

        verify(alertFactory).createAlert(AlertType.EXIT_GEOFENCE, cow, location);
        verify(alertRepository).save(alertToCreate);
        verify(domainMetricsService).incrementAlertCreated(AlertType.EXIT_GEOFENCE);
        ArgumentCaptor<NotificationMessage> notificationCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationDispatcher).dispatch(notificationCaptor.capture());
        assertEquals("HIGH", notificationCaptor.getValue().getSeverity());
        assertEquals("EXIT_GEOFENCE", notificationCaptor.getValue().getMetadata().get("alertType"));
    }

    @Test
    void shouldNotDuplicatePendingExitGeofenceAlert() {
        Alert existingAlert = new Alert();
        existingAlert.setId(200L);
        existingAlert.setCow(cow);
        existingAlert.setLocation(location);
        existingAlert.setType(AlertType.EXIT_GEOFENCE);
        existingAlert.setStatus(AlertStatus.PENDIENTE);

        when(alertRepository.findByCowAndTypeAndStatus(
                cow,
                AlertType.EXIT_GEOFENCE,
                AlertStatus.PENDIENTE
        )).thenReturn(Optional.of(existingAlert));

        Alert result = alertService.createExitGeofenceAlert(cow, location);

        assertNull(result);
        verify(alertFactory, never()).createAlert(any(), any(), any());
        verify(alertRepository, never()).save(any(Alert.class));
        verify(domainMetricsService, never()).incrementAlertCreated(any());
        verify(notificationDispatcher, never()).dispatch(any(NotificationMessage.class));
    }

    @Test
    void shouldCreateCollarOfflineAlertAndDispatchNotification() {
        Collar collar = new Collar();
        collar.setId(30L);
        collar.setToken("COLLAR-001");
        collar.setCow(cow);
        collar.setLastSeenAt(LocalDateTime.now().minusMinutes(20));

        when(alertRepository.findByCowAndTypeAndStatus(
                cow,
                AlertType.COLLAR_OFFLINE,
                AlertStatus.PENDIENTE
        )).thenReturn(Optional.empty());

        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert alert = invocation.getArgument(0);
            alert.setId(300L);
            return alert;
        });

        Alert created = alertService.createCollarOfflineAlert(collar);

        assertNotNull(created);
        assertEquals(AlertType.COLLAR_OFFLINE, created.getType());
        assertEquals(AlertStatus.PENDIENTE, created.getStatus());
        assertEquals(cow, created.getCow());
        assertTrue(created.getMessage().contains("COLLAR-001"));

        verify(alertRepository).save(any(Alert.class));
        verify(domainMetricsService).incrementAlertCreated(AlertType.COLLAR_OFFLINE);
        ArgumentCaptor<NotificationMessage> notificationCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationDispatcher).dispatch(notificationCaptor.capture());
        assertEquals("HIGH", notificationCaptor.getValue().getSeverity());
        assertEquals("COLLAR_OFFLINE", notificationCaptor.getValue().getMetadata().get("alertType"));
    }

    @Test
    void shouldNotDuplicatePendingCollarOfflineAlert() {
        Collar collar = new Collar();
        collar.setId(31L);
        collar.setToken("COLLAR-002");
        collar.setCow(cow);
        collar.setLastSeenAt(LocalDateTime.now().minusMinutes(25));

        Alert existingAlert = new Alert();
        existingAlert.setId(301L);
        existingAlert.setCow(cow);
        existingAlert.setType(AlertType.COLLAR_OFFLINE);
        existingAlert.setStatus(AlertStatus.PENDIENTE);

        when(alertRepository.findByCowAndTypeAndStatus(
                cow,
                AlertType.COLLAR_OFFLINE,
                AlertStatus.PENDIENTE
        )).thenReturn(Optional.of(existingAlert));

        Alert result = alertService.createCollarOfflineAlert(collar);

        assertNull(result);
        verify(alertRepository, never()).save(any(Alert.class));
        verify(domainMetricsService, never()).incrementAlertCreated(AlertType.COLLAR_OFFLINE);
        verify(notificationDispatcher, never()).dispatch(any(NotificationMessage.class));
    }

    @Test
    void shouldCreateLowBatteryAlertWhenCollarIsActiveEnabledAndBatteryAtThreshold() {
        Collar collar = new Collar();
        collar.setId(32L);
        collar.setToken("COLLAR-LOW-001");
        collar.setCow(cow);
        collar.setEnabled(true);
        collar.setStatus(CollarStatus.ACTIVO);
        collar.setBatteryLevel(20);

        when(alertRepository.findByCowAndTypeAndStatus(
                cow,
                AlertType.LOW_BATTERY,
                AlertStatus.PENDIENTE
        )).thenReturn(Optional.empty());

        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert alert = invocation.getArgument(0);
            alert.setId(302L);
            return alert;
        });

        Alert created = alertService.createLowBatteryAlert(collar);

        assertNotNull(created);
        assertEquals(AlertType.LOW_BATTERY, created.getType());
        assertEquals(AlertStatus.PENDIENTE, created.getStatus());
        assertEquals(cow, created.getCow());
        assertTrue(created.getMessage().contains("COLLAR-LOW-001"));
        assertTrue(created.getMessage().contains("20%"));
        verify(domainMetricsService).incrementAlertCreated(AlertType.LOW_BATTERY);
        ArgumentCaptor<NotificationMessage> notificationCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationDispatcher).dispatch(notificationCaptor.capture());
        assertEquals("MEDIUM", notificationCaptor.getValue().getSeverity());
        assertEquals("LOW_BATTERY", notificationCaptor.getValue().getMetadata().get("alertType"));
    }

    @Test
    void shouldNotDuplicatePendingLowBatteryAlert() {
        Collar collar = new Collar();
        collar.setId(33L);
        collar.setToken("COLLAR-LOW-002");
        collar.setCow(cow);
        collar.setEnabled(true);
        collar.setStatus(CollarStatus.ACTIVO);
        collar.setBatteryLevel(10);

        Alert existingAlert = new Alert();
        existingAlert.setId(303L);
        existingAlert.setCow(cow);
        existingAlert.setType(AlertType.LOW_BATTERY);
        existingAlert.setStatus(AlertStatus.PENDIENTE);

        when(alertRepository.findByCowAndTypeAndStatus(
                cow,
                AlertType.LOW_BATTERY,
                AlertStatus.PENDIENTE
        )).thenReturn(Optional.of(existingAlert));

        Alert result = alertService.createLowBatteryAlert(collar);

        assertNull(result);
        verify(alertRepository, never()).save(any(Alert.class));
        verify(domainMetricsService, never()).incrementAlertCreated(AlertType.LOW_BATTERY);
        verify(notificationDispatcher, never()).dispatch(any(NotificationMessage.class));
    }

    @Test
    void shouldNotCreateLowBatteryAlertWhenCollarIsDisabledOrInactive() {
        Collar disabledCollar = new Collar();
        disabledCollar.setToken("COLLAR-LOW-003");
        disabledCollar.setCow(cow);
        disabledCollar.setEnabled(false);
        disabledCollar.setStatus(CollarStatus.ACTIVO);
        disabledCollar.setBatteryLevel(15);

        Collar inactiveCollar = new Collar();
        inactiveCollar.setToken("COLLAR-LOW-004");
        inactiveCollar.setCow(cow);
        inactiveCollar.setEnabled(true);
        inactiveCollar.setStatus(CollarStatus.INACTIVO);
        inactiveCollar.setBatteryLevel(15);

        assertNull(alertService.createLowBatteryAlert(disabledCollar));
        assertNull(alertService.createLowBatteryAlert(inactiveCollar));

        verify(alertRepository, never()).save(any(Alert.class));
        verify(domainMetricsService, never()).incrementAlertCreated(AlertType.LOW_BATTERY);
    }

    @Test
    void shouldResolvePendingLowBatteryAlertWhenBatteryRecoversAboveThreshold() {
        Collar collar = new Collar();
        collar.setId(34L);
        collar.setToken("COLLAR-LOW-005");
        collar.setCow(cow);
        collar.setEnabled(true);
        collar.setStatus(CollarStatus.ACTIVO);
        collar.setBatteryLevel(30);

        Alert alert = new Alert();
        alert.setId(304L);
        alert.setType(AlertType.LOW_BATTERY);
        alert.setMessage("Batería baja");
        alert.setCreatedAt(LocalDateTime.now().minusMinutes(30));
        alert.setStatus(AlertStatus.PENDIENTE);
        alert.setObservations("Previa");
        alert.setCow(cow);

        when(alertRepository.findByCowAndTypeAndStatus(
                cow,
                AlertType.LOW_BATTERY,
                AlertStatus.PENDIENTE
        )).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Alert resolved = alertService.resolvePendingLowBatteryAlert(collar, LocalDateTime.now());

        assertNotNull(resolved);
        assertEquals(AlertStatus.RESUELTA, resolved.getStatus());
        assertTrue(resolved.getObservations().contains("30%"));
        verify(domainMetricsService).incrementAlertResolved(AlertType.LOW_BATTERY);
        verify(notificationDispatcher, never()).dispatch(any(NotificationMessage.class));
    }

    @Test
    void shouldResolveAlert() {
        Alert alert = new Alert();
        alert.setId(1L);
        alert.setType(AlertType.EXIT_GEOFENCE);
        alert.setMessage("Alerta de prueba");
        alert.setCreatedAt(LocalDateTime.now());
        alert.setStatus(AlertStatus.PENDIENTE);
        alert.setCow(cow);
        alert.setLocation(location);

        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AlertResponseDTO response = alertService.resolveAlert(1L, "Caso revisado");

        assertEquals("RESUELTA", response.getStatus());
        assertEquals("Caso revisado", response.getObservations());
        assertEquals("VACA-001", response.getCowToken());

        verify(alertRepository).save(any(Alert.class));
        verify(domainMetricsService).incrementAlertResolved(AlertType.EXIT_GEOFENCE);
    }

    @Test
    void shouldRejectResolvingDiscardedAlert() {
        Alert alert = createPersistedAlert(AlertStatus.DESCARTADA);

        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> alertService.resolveAlert(1L, "Intento invalido")
        );

        assertEquals("Transicion de estado de alerta no permitida: DESCARTADA -> RESUELTA", exception.getMessage());
        verify(alertRepository, never()).save(any(Alert.class));
        verify(auditLogService, never()).logWithCurrentActor(any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void shouldRejectDiscardingResolvedAlert() {
        Alert alert = createPersistedAlert(AlertStatus.RESUELTA);

        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> alertService.discardAlert(1L, "Intento invalido")
        );

        assertEquals("Transicion de estado de alerta no permitida: RESUELTA -> DESCARTADA", exception.getMessage());
        verify(alertRepository, never()).save(any(Alert.class));
        verify(auditLogService, never()).logWithCurrentActor(any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void shouldRejectReopeningTerminalAlertThroughUpdate() {
        Alert alert = createPersistedAlert(AlertStatus.RESUELTA);
        AlertUpdateRequestDTO request = new AlertUpdateRequestDTO();
        request.setStatus(AlertStatus.PENDIENTE);
        request.setObservations("Reabrir");

        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> alertService.updateAlert(1L, request)
        );

        assertEquals("Transicion de estado de alerta no permitida: RESUELTA -> PENDIENTE", exception.getMessage());
        verify(alertRepository, never()).save(any(Alert.class));
        verify(auditLogService, never()).logWithCurrentActor(any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void shouldReturnPagedAlertsWithFiltersAndSafeSort() {
        Alert alert = new Alert();
        alert.setId(50L);
        alert.setType(AlertType.COLLAR_OFFLINE);
        alert.setMessage("Collar sin reporte");
        alert.setCreatedAt(LocalDateTime.now());
        alert.setStatus(AlertStatus.PENDIENTE);
        alert.setCow(cow);

        when(alertRepository.findAll(anyAlertSpecification(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(alert)));
        when(alertPriorityScorer.score(alert, scoringContext)).thenReturn(new AlertPriorityAssessment(75, "HIGH"));

        Page<AlertResponseDTO> response = alertService.getAlertsPage(
                AlertStatus.PENDIENTE,
                AlertType.COLLAR_OFFLINE,
                0,
                20,
                "createdAt",
                "DESC"
        );

        assertEquals(1, response.getTotalElements());
        assertEquals("COLLAR_OFFLINE", response.getContent().get(0).getType());
        assertEquals("PENDIENTE", response.getContent().get(0).getStatus());
        assertEquals(75, response.getContent().get(0).getPriorityScore());
        assertEquals("HIGH", response.getContent().get(0).getPriority());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(alertRepository).findAll(anyAlertSpecification(), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());
        assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("createdAt").getDirection());
    }

    @Test
    void shouldRejectPagedAlertsWhenPageSizeIsTooLarge() {
        assertThrows(BadRequestException.class, () -> alertService.getAlertsPage(
                null,
                null,
                0,
                101,
                "createdAt",
                "DESC"
        ));
    }

    @Test
    void shouldRejectPagedAlertsWhenSortFieldIsNotAllowed() {
        assertThrows(BadRequestException.class, () -> alertService.getAlertsPage(
                null,
                null,
                0,
                20,
                "cow.password",
                "DESC"
        ));
    }

    @Test
    void shouldSortPendingAlertsByPriorityScoreWhenListingByStatus() {
        Alert lowerPriority = createPersistedAlert(AlertStatus.PENDIENTE);
        lowerPriority.setId(2L);
        lowerPriority.setCreatedAt(LocalDateTime.now().minusMinutes(5));

        Alert higherPriority = createPersistedAlert(AlertStatus.PENDIENTE);
        higherPriority.setId(3L);
        higherPriority.setCreatedAt(LocalDateTime.now().minusMinutes(20));

        when(alertRepository.findByStatus(AlertStatus.PENDIENTE)).thenReturn(List.of(lowerPriority, higherPriority));
        when(alertPriorityScorer.score(lowerPriority, scoringContext)).thenReturn(new AlertPriorityAssessment(45, "MEDIUM"));
        when(alertPriorityScorer.score(higherPriority, scoringContext)).thenReturn(new AlertPriorityAssessment(80, "HIGH"));

        List<AlertResponseDTO> response = alertService.getAlertsByStatus(AlertStatus.PENDIENTE);

        assertEquals(2, response.size());
        assertEquals(3L, response.get(0).getId());
        assertEquals("HIGH", response.get(0).getPriority());
        assertEquals(2L, response.get(1).getId());
    }

    @Test
    void shouldReturnPendingPriorityQueueOrderedAndLimited() {
        Alert lowerPriority = createPersistedAlert(AlertStatus.PENDIENTE);
        lowerPriority.setId(4L);
        lowerPriority.setCreatedAt(LocalDateTime.now().minusMinutes(5));

        Alert higherPriority = createPersistedAlert(AlertStatus.PENDIENTE);
        higherPriority.setId(5L);
        higherPriority.setCreatedAt(LocalDateTime.now().minusHours(2));

        when(alertRepository.findByStatus(AlertStatus.PENDIENTE)).thenReturn(List.of(lowerPriority, higherPriority));
        when(alertPriorityScorer.score(lowerPriority, scoringContext)).thenReturn(new AlertPriorityAssessment(45, "MEDIUM"));
        when(alertPriorityScorer.score(higherPriority, scoringContext)).thenReturn(new AlertPriorityAssessment(80, "HIGH"));

        List<AlertResponseDTO> response = alertService.getPendingAlertPriorityQueue(1);

        assertEquals(1, response.size());
        assertEquals(5L, response.get(0).getId());
        assertEquals("HIGH", response.get(0).getPriority());
    }

    @Test
    void shouldUseFullPendingSetWhenCandidateBudgetCoversAllAlerts() {
        Alert lowerPriority = createPersistedAlert(AlertStatus.PENDIENTE);
        lowerPriority.setId(6L);
        lowerPriority.setCreatedAt(LocalDateTime.now().minusMinutes(5));

        Alert higherPriority = createPersistedAlert(AlertStatus.PENDIENTE);
        higherPriority.setId(7L);
        higherPriority.setCreatedAt(LocalDateTime.now().minusHours(2));

        when(alertRepository.countByStatus(AlertStatus.PENDIENTE)).thenReturn(2L);
        when(alertRepository.findByStatus(AlertStatus.PENDIENTE)).thenReturn(List.of(lowerPriority, higherPriority));
        when(alertPriorityScorer.score(lowerPriority, scoringContext)).thenReturn(new AlertPriorityAssessment(45, "MEDIUM"));
        when(alertPriorityScorer.score(higherPriority, scoringContext)).thenReturn(new AlertPriorityAssessment(80, "HIGH"));

        List<AlertResponseDTO> response = alertService.getPendingAlertPriorityQueue(1);

        assertEquals(1, response.size());
        assertEquals(7L, response.get(0).getId());
        verify(alertRepository).findByStatus(AlertStatus.PENDIENTE);
        verify(alertRepository, never()).findByStatusOrderByCreatedAtAscIdAsc(eq(AlertStatus.PENDIENTE), any(Pageable.class));
        verify(alertRepository, never()).findByStatusOrderByCreatedAtDescIdDesc(eq(AlertStatus.PENDIENTE), any(Pageable.class));
    }

    @Test
    void shouldUseCandidateWindowsWhenPendingQueueExceedsBudget() {
        Alert oldestCandidate = createPersistedAlert(AlertStatus.PENDIENTE);
        oldestCandidate.setId(8L);
        oldestCandidate.setCreatedAt(LocalDateTime.now().minusHours(8));

        Alert newestCandidate = createPersistedAlert(AlertStatus.PENDIENTE);
        newestCandidate.setId(9L);
        newestCandidate.setCreatedAt(LocalDateTime.now().minusMinutes(2));

        when(alertRepository.countByStatus(AlertStatus.PENDIENTE)).thenReturn(500L);
        when(alertRepository.findByStatusOrderByCreatedAtAscIdAsc(eq(AlertStatus.PENDIENTE), any(Pageable.class)))
                .thenReturn(List.of(oldestCandidate));
        when(alertRepository.findByStatusOrderByCreatedAtDescIdDesc(eq(AlertStatus.PENDIENTE), any(Pageable.class)))
                .thenReturn(List.of(newestCandidate));
        when(alertPriorityScorer.score(oldestCandidate, scoringContext)).thenReturn(new AlertPriorityAssessment(70, "HIGH"));
        when(alertPriorityScorer.score(newestCandidate, scoringContext)).thenReturn(new AlertPriorityAssessment(55, "MEDIUM"));

        List<AlertResponseDTO> response = alertService.getPendingAlertPriorityQueue(10);

        assertEquals(2, response.size());
        assertEquals(8L, response.get(0).getId());
        assertEquals(9L, response.get(1).getId());

        ArgumentCaptor<Pageable> oldestPageCaptor = ArgumentCaptor.forClass(Pageable.class);
        ArgumentCaptor<Pageable> newestPageCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(alertRepository).findByStatusOrderByCreatedAtAscIdAsc(eq(AlertStatus.PENDIENTE), oldestPageCaptor.capture());
        verify(alertRepository).findByStatusOrderByCreatedAtDescIdDesc(eq(AlertStatus.PENDIENTE), newestPageCaptor.capture());
        verify(alertRepository, never()).findByStatus(AlertStatus.PENDIENTE);
        assertEquals(25, oldestPageCaptor.getValue().getPageSize());
        assertEquals(25, newestPageCaptor.getValue().getPageSize());
    }

    @Test
    void shouldRejectLegacyAllAlertsWhenResultExceedsMaximum() {
        when(alertRepository.count()).thenReturn(101L);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> alertService.getAllAlertsLegacy()
        );

        assertEquals(
                "La consulta legacy de alertas supera el maximo de 100 resultados. Usa /api/alerts/page para paginar.",
                exception.getMessage()
        );
        verify(alertRepository, never()).findAll();
    }

    @Test
    void shouldRejectLegacyAlertsByTypeWhenResultExceedsMaximum() {
        when(alertRepository.countByType(AlertType.COLLAR_OFFLINE)).thenReturn(101L);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> alertService.getAlertsByTypeLegacy(AlertType.COLLAR_OFFLINE)
        );

        assertEquals(
                "La consulta legacy de alertas por tipo supera el maximo de 100 resultados. Usa /api/alerts/page para paginar.",
                exception.getMessage()
        );
        verify(alertRepository, never()).findByType(AlertType.COLLAR_OFFLINE);
    }

    @Test
    void shouldRejectPendingPriorityQueueWhenLimitIsInvalid() {
        assertThrows(BadRequestException.class, () -> alertService.getPendingAlertPriorityQueue(0));
    }

    @SuppressWarnings("unchecked")
    private Specification<Alert> anyAlertSpecification() {
        return any(Specification.class);
    }

    private Alert createPersistedAlert(AlertStatus status) {
        Alert alert = new Alert();
        alert.setId(1L);
        alert.setType(AlertType.EXIT_GEOFENCE);
        alert.setMessage("Alerta de prueba");
        alert.setCreatedAt(LocalDateTime.now());
        alert.setStatus(status);
        alert.setCow(cow);
        alert.setLocation(location);
        return alert;
    }
}
