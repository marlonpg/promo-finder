package dev.gamba.promofinder.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** One Claude search for a watch: when, how it went and what it cost. */
@Entity
@Table(indexes = @Index(columnList = "watchId"))
public class CheckRun {

	@Id
	@GeneratedValue
	private Long id;

	@Column(nullable = false)
	private Long watchId;

	private Instant startedAt;

	private Instant finishedAt;

	private String errorCode;

	@Column(length = 2000)
	private String note;

	@Column(precision = 12, scale = 2)
	private BigDecimal bestPrice;

	private String bestStore;

	private int matches;

	private Double costUsd;

	protected CheckRun() {
	}

	public CheckRun(Long watchId, Instant startedAt) {
		this.watchId = watchId;
		this.startedAt = startedAt;
	}

	public boolean isSuccess() {
		return errorCode == null;
	}

	public Long getId() { return id; }
	public Long getWatchId() { return watchId; }
	public Instant getStartedAt() { return startedAt; }
	public Instant getFinishedAt() { return finishedAt; }
	public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
	public String getErrorCode() { return errorCode; }
	public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
	public String getNote() { return note; }
	public void setNote(String note) { this.note = note; }
	public BigDecimal getBestPrice() { return bestPrice; }
	public void setBestPrice(BigDecimal bestPrice) { this.bestPrice = bestPrice; }
	public String getBestStore() { return bestStore; }
	public void setBestStore(String bestStore) { this.bestStore = bestStore; }
	public int getMatches() { return matches; }
	public void setMatches(int matches) { this.matches = matches; }
	public Double getCostUsd() { return costUsd; }
	public void setCostUsd(Double costUsd) { this.costUsd = costUsd; }
}
