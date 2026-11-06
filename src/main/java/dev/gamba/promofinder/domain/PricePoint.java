package dev.gamba.promofinder.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** The best price one store had for a watch on one check, whether or not it hit the target. */
@Entity
@Table(indexes = { @Index(columnList = "watchId"), @Index(columnList = "checkRunId") })
public class PricePoint {

	@Id
	@GeneratedValue
	private Long id;

	@Column(nullable = false)
	private Long watchId;

	@Column(nullable = false)
	private Long checkRunId;

	private String store;

	@Column(length = 500)
	private String title;

	@Column(precision = 12, scale = 2)
	private BigDecimal price;

	@Column(length = 2000)
	private String url;

	private Instant checkedAt;

	protected PricePoint() {
	}

	public PricePoint(Long watchId, Long checkRunId, String store, String title, BigDecimal price, String url,
			Instant checkedAt) {
		this.watchId = watchId;
		this.checkRunId = checkRunId;
		this.store = store;
		this.title = title;
		this.price = price;
		this.url = url;
		this.checkedAt = checkedAt;
	}

	public Long getId() { return id; }
	public Long getWatchId() { return watchId; }
	public Long getCheckRunId() { return checkRunId; }
	public String getStore() { return store; }
	public String getTitle() { return title; }
	public BigDecimal getPrice() { return price; }
	public String getUrl() { return url; }
	public Instant getCheckedAt() { return checkedAt; }
}
