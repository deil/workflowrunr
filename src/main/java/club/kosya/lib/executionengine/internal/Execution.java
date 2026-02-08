package club.kosya.lib.executionengine.internal;

import club.kosya.lib.executionengine.ExecutionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "executions")
public class Execution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status;

    @Column(nullable = false)
    private LocalDateTime queuedAt;

    @Column(nullable = false)
    private byte[] definition;

    @Column(nullable = false)
    private String params;

    private String state;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private Instant wakeAt;
}
