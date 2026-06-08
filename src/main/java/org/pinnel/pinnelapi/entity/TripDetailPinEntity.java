package org.pinnel.pinnelapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "trip_detail_pins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripDetailPinEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_detail_id", nullable = false)
    private TripDetailEntity tripDetail;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pin_id", nullable = false)
    private PinEntity pin;

    @Column(name = "pin_order", nullable = false)
    private Integer pinOrder;

    @Column(name = "visit_time")
    private LocalTime visitTime;

    @Column(precision = 12, scale = 2)
    private BigDecimal budget;
}
