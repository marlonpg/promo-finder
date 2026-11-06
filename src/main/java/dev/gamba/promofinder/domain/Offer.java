package dev.gamba.promofinder.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** An offer at or below the target price that the monitor found. */
@Entity
@Table(indexes = @Index(columnList = "watchId"))
public class Offer {

	@Id
	@GeneratedValue
	private Long id;

	@Column(nullable = false)
	private Long watchId;

	private String store;

	@Column(length = 500)
	private String title;

	@Column(precision = 12, scale = 2)
	private BigDecimal price;

	@Column(length = 2000)
	private String url;

	private Instant foundAt;

	private boolean notified;

	protected Offer() {
	}

	public Offer(Long watchId, String store, String title, BigDecimal price, String url, Instant foundAt) {
		this.watchId = watchId;
		this.store = store;
		this.title = title;
		this.price = price;
		this.url = url;
		this.foundAt = foundAt;
	}

	public Long getId() { return id; }
	public Long getWatchId() { return watchId; }
	public String getStore() { return store; }
	public String getTitle() { return title; }
	public BigDecimal getPrice() { return price; }
	public String getUrl() { return url; }
	public Instant getFoundAt() { return foundAt; }
	public boolean isNotified() { return notified; }
	public void setNotified(boolean notified) { this.notified = notified; }
}
