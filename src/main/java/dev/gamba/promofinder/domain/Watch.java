package dev.gamba.promofinder.domain;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/** "Tell me when <product> costs at most <maxPrice>", checked every <intervalMinutes>. */
@Entity
public class Watch {

	@Id
	@GeneratedValue
	private Long id;

	@Column(nullable = false)
	private String product;

	/** Free-text constraints for the AI, e.g. "new, 512GB, any color, no marketplace sellers". */
	@Column(length = 1000)
	private String details;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal maxPrice;

	private int intervalMinutes = 60;

	private boolean active = true;

	@ElementCollection(fetch = FetchType.EAGER)
	@Enumerated(EnumType.STRING)
	private Set<NotificationChannel> channels = EnumSet.noneOf(NotificationChannel.class);

	private Instant createdAt = Instant.now();

	private Instant lastCheckedAt;

	/** Claude's short note about the last check, or the technical error detail. */
	@Column(length = 2000)
	private String lastStatus;

	/** Set when the last check failed; see {@code CheckError}. */
	private String lastErrorCode;

	/** Cheapest price seen on the last successful check, whether or not it hit the target. */
	@Column(precision = 12, scale = 2)
	private BigDecimal lastBestPrice;

	private String lastBestStore;

	@Column(length = 2000)
	private String lastBestUrl;

	/** Soft delete, so the UI can offer "Undo". Purged by the scheduler a few minutes later. */
	private Instant deletedAt;

	public boolean isDue(Instant now) {
		return active && deletedAt == null && (lastCheckedAt == null || !nextCheckAt().isAfter(now));
	}

	public Instant nextCheckAt() {
		return lastCheckedAt == null ? null : lastCheckedAt.plus(Duration.ofMinutes(intervalMinutes));
	}

	public boolean isDeal() {
		return lastErrorCode == null && lastBestPrice != null && lastBestPrice.compareTo(maxPrice) <= 0;
	}

	public Long getId() { return id; }
	public String getProduct() { return product; }
	public void setProduct(String product) { this.product = product; }
	public String getDetails() { return details; }
	public void setDetails(String details) { this.details = details; }
	public BigDecimal getMaxPrice() { return maxPrice; }
	public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }
	public int getIntervalMinutes() { return intervalMinutes; }
	public void setIntervalMinutes(int intervalMinutes) { this.intervalMinutes = intervalMinutes; }
	public boolean isActive() { return active; }
	public void setActive(boolean active) { this.active = active; }
	public Set<NotificationChannel> getChannels() { return channels; }
	public void setChannels(Set<NotificationChannel> channels) { this.channels = channels; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getLastCheckedAt() { return lastCheckedAt; }
	public void setLastCheckedAt(Instant lastCheckedAt) { this.lastCheckedAt = lastCheckedAt; }
	public String getLastStatus() { return lastStatus; }
	public void setLastStatus(String lastStatus) { this.lastStatus = lastStatus; }
	public String getLastErrorCode() { return lastErrorCode; }
	public void setLastErrorCode(String lastErrorCode) { this.lastErrorCode = lastErrorCode; }
	public BigDecimal getLastBestPrice() { return lastBestPrice; }
	public void setLastBestPrice(BigDecimal lastBestPrice) { this.lastBestPrice = lastBestPrice; }
	public String getLastBestStore() { return lastBestStore; }
	public void setLastBestStore(String lastBestStore) { this.lastBestStore = lastBestStore; }
	public String getLastBestUrl() { return lastBestUrl; }
	public void setLastBestUrl(String lastBestUrl) { this.lastBestUrl = lastBestUrl; }
	public Instant getDeletedAt() { return deletedAt; }
	public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
