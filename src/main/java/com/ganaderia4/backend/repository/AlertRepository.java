package com.ganaderia4.backend.repository;

import com.ganaderia4.backend.model.Alert;
import com.ganaderia4.backend.model.AlertStatus;
import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.model.Cow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Long>, JpaSpecificationExecutor<Alert> {

    List<Alert> findByStatus(AlertStatus status);

    List<Alert> findByType(AlertType type);

    Optional<Alert> findByCowAndTypeAndStatus(Cow cow, AlertType type, AlertStatus status);

    long countByCow(Cow cow);

    long countByStatus(AlertStatus status);

    long countByStatusAndCreatedAtBefore(AlertStatus status, java.time.LocalDateTime createdAt);

    long countByTypeAndStatus(AlertType type, AlertStatus status);

    List<Alert> findTop10ByStatusOrderByCreatedAtDesc(AlertStatus status);

    @Query("""
            select a.cow.id, count(a)
            from Alert a
            where a.cow.id in :cowIds
            group by a.cow.id
            """)
    List<Object[]> countGroupedByCowIds(@Param("cowIds") Collection<Long> cowIds);

    @Query("""
            select new com.ganaderia4.backend.repository.CowIncidentAggregateView(
                a.cow.id,
                a.cow.token,
                a.cow.name,
                a.cow.status,
                count(a),
                sum(case when a.status = com.ganaderia4.backend.model.AlertStatus.PENDIENTE then 1 else 0 end),
                sum(case when a.status = com.ganaderia4.backend.model.AlertStatus.RESUELTA then 1 else 0 end),
                sum(case when a.status = com.ganaderia4.backend.model.AlertStatus.DESCARTADA then 1 else 0 end),
                min(a.createdAt),
                max(a.createdAt)
            )
            from Alert a
            where a.cow is not null
              and a.createdAt >= coalesce(:from, a.createdAt)
              and a.createdAt <= coalesce(:to, a.createdAt)
              and a.type = coalesce(:type, a.type)
              and a.status = coalesce(:status, a.status)
            group by a.cow.id, a.cow.token, a.cow.name, a.cow.status
            order by count(a) desc, max(a.createdAt) desc
            """)
    List<CowIncidentAggregateView> findCowIncidentAggregatesByTotalIncidents(
            @Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to,
            @Param("type") AlertType type,
            @Param("status") AlertStatus status,
            Pageable pageable
    );

    @Query("""
            select new com.ganaderia4.backend.repository.CowIncidentAggregateView(
                a.cow.id,
                a.cow.token,
                a.cow.name,
                a.cow.status,
                count(a),
                sum(case when a.status = com.ganaderia4.backend.model.AlertStatus.PENDIENTE then 1 else 0 end),
                sum(case when a.status = com.ganaderia4.backend.model.AlertStatus.RESUELTA then 1 else 0 end),
                sum(case when a.status = com.ganaderia4.backend.model.AlertStatus.DESCARTADA then 1 else 0 end),
                min(a.createdAt),
                max(a.createdAt)
            )
            from Alert a
            where a.cow is not null
              and a.createdAt >= coalesce(:from, a.createdAt)
              and a.createdAt <= coalesce(:to, a.createdAt)
              and a.type = coalesce(:type, a.type)
              and a.status = coalesce(:status, a.status)
            group by a.cow.id, a.cow.token, a.cow.name, a.cow.status
            order by
                sum(case when a.status = com.ganaderia4.backend.model.AlertStatus.PENDIENTE then 1 else 0 end) desc,
                max(a.createdAt) desc,
                count(a) desc
            """)
    List<CowIncidentAggregateView> findCowIncidentAggregatesByOperationalRecurrence(
            @Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to,
            @Param("type") AlertType type,
            @Param("status") AlertStatus status,
            Pageable pageable
    );

    @Query("""
            select a.cow.id, a.type
            from Alert a
            where a.cow.id in :cowIds
              and a.createdAt >= coalesce(:from, a.createdAt)
              and a.createdAt <= coalesce(:to, a.createdAt)
              and a.type = coalesce(:type, a.type)
              and a.status = coalesce(:status, a.status)
            order by a.cow.id asc, a.createdAt desc, a.id desc
            """)
    List<Object[]> findLatestIncidentTypesByCowIds(
            @Param("cowIds") Collection<Long> cowIds,
            @Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to,
            @Param("type") AlertType type,
            @Param("status") AlertStatus status
    );
}
