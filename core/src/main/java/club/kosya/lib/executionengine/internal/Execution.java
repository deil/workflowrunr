package club.kosya.lib.executionengine.internal;

import club.kosya.lib.executionengine.ExecutionStatus;
import java.time.Instant;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;
import lombok.Data;

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

    @Version
    private Long version;
}
