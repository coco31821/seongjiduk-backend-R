package com.sungjiduk.backend.payment.entity;

import com.sungjiduk.backend.common.BaseEntity;
import com.sungjiduk.backend.payment.constants.TravelOfferType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 주문에 포함된 항공권/숙소 항목.
 * TravelOffer가 바뀌어도 결제 당시 이름과 금액을 유지하기 위해 itemName/amount를 별도로 저장한다.
 */
@Entity
@Table(name = "payment_order_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentOrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_order_id", nullable = false)
    private PaymentOrder paymentOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "travel_offer_id", nullable = false)
    private TravelOffer travelOffer;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private TravelOfferType itemType;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Builder
    private PaymentOrderItem(TravelOffer travelOffer, String itemName, TravelOfferType itemType,
                             Long amount, String currency) {
        this.travelOffer = travelOffer;
        this.itemName = itemName;
        this.itemType = itemType;
        this.amount = amount;
        this.currency = currency == null ? "KRW" : currency;
    }

    public static PaymentOrderItem from(TravelOffer offer) {
        return PaymentOrderItem.builder()
                .travelOffer(offer)
                .itemName(offer.getName())
                .itemType(offer.getOfferType())
                .amount(offer.getPrice())
                .currency(offer.getCurrency())
                .build();
    }

    void assignOrder(PaymentOrder paymentOrder) {
        this.paymentOrder = paymentOrder;
    }
}
